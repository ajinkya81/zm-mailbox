/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavResponse;
import com.zimbra.cs.dav.service.DavResponse.PropStat;

/*
 * rfc 3253 section 3.8
 * 
 *      The request body MUST be a DAV:expand-property XML element.
 *
 *      <!ELEMENT expand-property (property*)>
 *      <!ELEMENT property (property*)>
 *      <!ATTLIST property name NMTOKEN #REQUIRED>
 *      name value: a property element type
 *      <!ATTLIST property namespace NMTOKEN "DAV:">
 *      namespace value: an XML namespace
 *
 *      The response body for a successful request MUST be a
 *      DAV:multistatus XML element.
 *                                
 */
public class ExpandProperty extends Report {
	public void handle(DavContext ctxt) throws ServiceException, DavException {
		Element query = ctxt.getRequestMessage().getRootElement();
		if (!query.getQName().equals(DavElements.E_EXPAND_PROPERTY))
			throw new DavException("msg "+query.getName()+" is not expand-property", HttpServletResponse.SC_BAD_REQUEST, null);

		DavResource rs = ctxt.getRequestedResource();
		Element resp = ctxt.getDavResponse().getTop(DavElements.E_MULTISTATUS).addElement(DavElements.E_RESPONSE);
		expandProperties(ctxt, rs, query, resp);
		
		ArrayList<String> hrefs = new ArrayList<String>();
		for (Object obj : query.elements(DavElements.E_HREF)) {
			if (obj instanceof Element) {
				String href = ((Element)obj).getText();
				try {
					href = URLDecoder.decode(href, "UTF-8");
				} catch (IOException e) {
					ZimbraLog.dav.warn("can't decode href "+href, e);
				}
				hrefs.add(href);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void expandProperties(DavContext ctxt, DavResource rs, Element elem, Element resp) {
		rs.getProperty(DavElements.E_HREF).toElement(ctxt, resp, false);
		Iterator iter = elem.elementIterator(DavElements.E_PROPERTY);
		PropStat propstat = new PropStat();
		while (iter.hasNext()) {
			Element property = (Element)iter.next();
			Prop p = new Prop(property);
			ResourceProperty rp = rs.getProperty(p.getQName());
			if (rp == null) {
				propstat.add(p.getQName(), null, HttpServletResponse.SC_NOT_FOUND);
			} else {
				Iterator subProps = property.elementIterator();
				if (subProps.hasNext()) {
					PropStat sub = new PropStat();
					sub.add(rp);
					Element subElem = DocumentHelper.createElement(DavElements.E_RESPONSE);
					sub.toResponse(ctxt, subElem, false);
					Iterator subPropstats = subElem.elementIterator(DavElements.E_PROPSTAT);
					while (subPropstats.hasNext()) {
						Element subPropstat = (Element)subPropstats.next();
						Element status = subPropstat.element(DavElements.E_STATUS);
						if (!status.getText().equals(DavResponse.sStatusTextMap.get(HttpServletResponse.SC_OK)))
							continue;
						Element prop = subPropstat.element(DavElements.E_PROP);
						if (prop == null)
							continue;
						prop = prop.element(p.getQName());
						if (prop == null)
							continue;
						Element href = prop.element(DavElements.E_HREF);
						if (href == null)
							continue;
						String url = href.getText();
						if (url == null)
							continue;
						try {
							DavResource target = UrlNamespace.getResourceAtUrl(ctxt, url);
							href.detach();
							Element targetElem = DocumentHelper.createElement(DavElements.E_RESPONSE);
							expandProperties(ctxt, target, property, targetElem);
							propstat.add(rp.getName(), targetElem);
						} catch (DavException e) {
					        ZimbraLog.dav.warn("can't find resource for "+url, e);
						}
					}
				} else {
					propstat.add(rp);
				}
			}
		}
		propstat.toResponse(ctxt, resp, false);
	}
	private static class Prop {
		private String mName;
		private String mNamespace;
		private QName mQName;
		private Element mElement;
		
		public Prop(Element propElement) {
			mName = propElement.attributeValue(DavElements.P_NAME);
			mNamespace = propElement.attributeValue(DavElements.P_NAMESPACE, DavElements.WEBDAV_NS_STRING);
			mQName = QName.get(mName, mNamespace);
			mElement = propElement;
		}
		public QName getQName() {
			return mQName;
		}
		public Element getElement() {
			return mElement;
		}
	}
}
