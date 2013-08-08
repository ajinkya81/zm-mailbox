/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.util.ZimbraLog;

public class ServerThrottle {

    private static final ConcurrentMap<String, ServerThrottle> instances = new ConcurrentHashMap<String, ServerThrottle>();

    public static ServerThrottle getThrottle(String serverType) {
        return instances.get(serverType);
    }

    public static synchronized void configureThrottle(String serverType, int ipReqLimit, int acctReqLimit,
            Iterable<String> safeHosts, Iterable<String> whitelistHosts) {
        ServerThrottle throttle = getThrottle(serverType);
        if (throttle == null) {
            throttle = new ServerThrottle(serverType);
            instances.put(serverType, throttle);
        }
        throttle.setIpReqsPerSecond(ipReqLimit);
        throttle.setAcctReqsPerSecond(acctReqLimit);
        for (String hostname : safeHosts) {
            throttle.addToHostList(hostname, false);
        }
        for (String hostname : whitelistHosts) {
            throttle.addToHostList(hostname, true);
        }
    }

    @VisibleForTesting
    ServerThrottle(String serverType) {
        this.serverType = serverType;
    }

    private String serverType;

    private int ipReqsPerSecond = 0; // max reqs/second per IP

    private int acctReqsPerSecond = 0; // max reqs/second per account

    private ConcurrentMap<String, List<Long>> ipReqs = new ConcurrentHashMap<String, List<Long>>();
    // map containing a list of the timestamps for prior requests by ip

    private ConcurrentMap<String, List<Long>> acctReqs = new ConcurrentHashMap<String, List<Long>>();
    // map containing a list of the timestamps for prior requests by acct

    private Set<String> ignoredIps = new HashSet<String>();

    private Set<String> whitelistIps = new HashSet<String>();

    @VisibleForTesting
    void setIpReqsPerSecond(int ipReqsPerSecond) {
        this.ipReqsPerSecond = ipReqsPerSecond;
    }

    @VisibleForTesting
    void setAcctReqsPerSecond(int acctReqsPerSecond) {
        this.acctReqsPerSecond = acctReqsPerSecond;
    }

    @VisibleForTesting
    void addIgnoredIp(String ip) {
        ZimbraLog.net.debug("adding IP %s to throttle ignore list", ip);
        ignoredIps.add(ip);
    }

    @VisibleForTesting
    void addWhitelistIp(String ip) {
        ZimbraLog.net.debug("adding IP %s to throttle whitelist", ip);
        whitelistIps.add(ip);
        addIgnoredIp(ip); //whitelist implicitly ignored; wire it up here so we don't have to check everywhere
    }

    private void addToHostList(String hostname, boolean whitelist) {
        try {
            InetAddress[] addrs = InetAddress.getAllByName(hostname);
            if (addrs != null) {
                for (InetAddress addr : addrs) {
                    if (whitelist) {
                        addWhitelistIp(addr.getHostAddress());
                    } else {
                        addIgnoredIp(addr.getHostAddress());

                    }
                }
            }
        } catch (UnknownHostException e) {
            ZimbraLog.net.warn("unknown host %s cannot be added to throttle %slist." +
            		" %s requests from this host may be throttled. " +
            		"If this host is a proxy please add it to your DNS.", (whitelist ? "white" : "ignore "), hostname, serverType);
        }
    }

    public boolean isIpThrottled(String ip) {
        if (ip == null) {
            return false;
        } else if (isIpInSet(ip, ignoredIps)) {
            return false;
        } else {
            return isThrottled(ipReqs, ip, ipReqsPerSecond);
        }
    }

    public boolean isIpWhitelisted(String ip) {
        if (ip == null) {
            return false;
        } else {
            return (isIpInSet(ip, whitelistIps));
        }
    }

    public boolean isAccountThrottled(String acctId, String ...requestIps) {
        for (String ip : requestIps) {
            if (isIpWhitelisted(ip)) {
                return false;
            }
        }
        return isThrottled(acctReqs, acctId, acctReqsPerSecond);
    }

    private boolean isIpInSet(String ip, Set<String> ips) {
        if (ip == null) {
            return false;
        } else if (ips.contains(ip)) {
            return true;
        } else if (ip.indexOf('%') >= 0 && ips.contains(ip.substring(0, ip.indexOf('%')))) {
            // edge case with IPv6 scoped interface; the client
            // connection is scoped but InetAddress.getAllByName()
            // returned unscoped address
            return true;
        } else {
            return false;
        }
    }

    private boolean isThrottled(Map<String, List<Long>> reqMap, String key, int limit) {
        if (limit <= 0) {
            return false;
        }
        List<Long> reqs = getReqs(key, reqMap);
        synchronized (reqs) {
            // ironically, this has a small throttling effect built in; two reqs
            // from the same acct or ip can't get past this concurrently
            reqs.add(System.currentTimeMillis());
            if (reqs.size() > limit) {
                pruneStaleRequests(reqs);
                return (reqs.size() > limit);
            } else {
                return false;
            }
        }
    }

    private void pruneStaleRequests(List<Long> reqTimes) {
        long now = System.currentTimeMillis();
        Iterator<Long> it = reqTimes.iterator();
        while (it.hasNext()) {
            Long time = it.next();
            if (time < now - 1000) {
                it.remove();
            } else {
                break; // currently ordered by List; watch this if collection
                // changes
            }
        }
    }

    private List<Long> getReqs(String key, Map<String, List<Long>> reqMap) {
        List<Long> reqs = reqMap.get(key);
        if (reqs == null) {
            // race here; duplicate initialization is tolerable since it only
            // makes our count slightly inaccurate for the first second. a real
            // flood will be detected +/- a few requests
            reqs = new ArrayList<Long>();
            reqMap.put(key, reqs);
        }
        return reqs;
    }

    @VisibleForTesting
    void addIpReq(String ip, Long time) {
        addReq(ip, ipReqs, time);
    }

    @VisibleForTesting
    void addAcctReq(String ip, Long time) {
        addReq(ip, acctReqs, time);
    }

    private void addReq(String key, Map<String, List<Long>> reqMap, Long time) {
        List<Long> reqs = getReqs(key, reqMap);
        synchronized (reqs) {
            reqs.add(time);
        }
    }
}
