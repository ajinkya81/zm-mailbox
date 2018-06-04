/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.RandomStringUtils;

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.ZAttrProvisioning.PrefPasswordRecoveryAddressStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.EmailRecoveryCode;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SendRecoveryCode;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest.Op;
import com.zimbra.soap.mail.message.SetRecoveryEmailResponse;
import com.zimbra.soap.type.Channel;

public class SetRecoveryEmail extends DocumentHandler {
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        SetRecoveryEmailRequest req = zsc.elementToJaxb(request);
        if (!mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraFeatureResetPasswordEnabled,
            false)) {
            throw ServiceException.PERM_DENIED("password reset feature not enabled.");
        }
        Channel channel = req.getChannel();
        if (channel == null) {
            throw ServiceException.INVALID_REQUEST("Invalid channel received.", null);
        }
        Op op = req.getOp();
        if (op == null) {
            throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        switch (channel) {
            case EMAIL:
                switch (op) {
                    case sendCode:
                        String recoveryEmailAddr = req.getRecoveryAccount();
                        if (StringUtil.isNullOrEmpty(recoveryEmailAddr)) {
                            throw ServiceException.INVALID_REQUEST("Recovery email address not provided.",
                                null);
                        }
                        validateEmail(recoveryEmailAddr, account);
                        sendCode(recoveryEmailAddr, 0, account, mbox, zsc, octxt, channel);
                        break;
                    case validateCode:
                        String recoveryAccountVerificationCode = req
                            .getRecoveryAccountVerificationCode();
                        if (StringUtil.isNullOrEmpty(recoveryAccountVerificationCode)) {
                            throw ServiceException.INVALID_REQUEST(
                                "Recovery email address verification code not provided.", null);
                        }
                        validateCode(recoveryAccountVerificationCode, account, mbox, zsc, octxt);
                        break;
                    case resendCode:
                        resendCode(account, mbox, zsc, octxt, channel);
                        break;
                    case reset:
                        reset(mbox, zsc);
                        break;
                    default:
                        throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
                    }
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid channel received.", null);
        }
        SetRecoveryEmailResponse resp = new SetRecoveryEmailResponse();
        return zsc.jaxbToElement(resp);
    }

    protected void sendCode(String email, int resendCount, Account account, Mailbox mbox,
        ZimbraSoapContext zsc, OperationContext octxt, Channel channel) throws ServiceException {
        String verificationData = account.getRecoveryEmailVerificationData();
        if (verificationData != null) {
            Map<String, String> recoveryDataMap = JWEUtil.getDecodedJWE(verificationData);
            String recoveryEmail = recoveryDataMap.get(CodeConstants.EMAIL.toString());
            if (ZimbraLog.passwordreset.isDebugEnabled()) {
                ZimbraLog.passwordreset.debug("sendCode: existing recovery email: %s", recoveryEmail);
            }
            if (resendCount == 0 && recoveryEmail.equals(email)) {
                throw ServiceException.INVALID_REQUEST(
                    "Verification code already sent to this recovery email.", null);
            }
        }
        String code = RandomStringUtils.random(8, true, true);
        Account authAccount = getAuthenticatedAccount(zsc);
        long expiry = account.getRecoveryEmailCodeValidity();
        Date now = new Date();
        long expiryTime = now.getTime() + expiry;
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put(CodeConstants.EMAIL.toString(), email);
        dataMap.put(CodeConstants.CODE.toString(), code);
        dataMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(expiryTime));
        dataMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
        SendRecoveryCode sendRecoveryCode = null;
        switch (channel) {
            case EMAIL:
                sendRecoveryCode = new EmailRecoveryCode(dataMap, mbox, authAccount);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid channel received.", null);
        }
        sendRecoveryCode.sendRecoveryAccountValidationCode(account, octxt);
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, email);
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus,
            PrefPasswordRecoveryAddressStatus.pending);
        String verificationDataStr = JWEUtil.getJWE(dataMap);
        prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, verificationDataStr);
        Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());
    }

    protected void validateCode(String recoveryAccountVerificationCode, Account account,
        Mailbox mbox, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        String verificationData = account.getRecoveryEmailVerificationData();
        if (verificationData == null) {
            throw ServiceException
            .FAILURE("The recovery email address verification data is missing.", null);
        }
        Map<String, String> recoveryDataMap = JWEUtil.getDecodedJWE(verificationData);
        String code = recoveryDataMap.get(CodeConstants.CODE.toString());
        long expiryTime = Long.parseLong(recoveryDataMap.get(CodeConstants.EXPIRY_TIME.toString()));
        if (ZimbraLog.passwordreset.isDebugEnabled()) {
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String gmtDate = format.format(expiryTime);
            ZimbraLog.passwordreset.debug("validateCode: expiryTime for code: %s", gmtDate);
            ZimbraLog.passwordreset.debug("ValidateCode: last 3 characters of recovery code: %s",
                code.substring(5));
        }
        Date now = new Date();
        if (expiryTime < now.getTime()) {
            throw ServiceException
                .FAILURE("The recovery email address verification code is expired.", null);

        }
        if (code.equals(recoveryAccountVerificationCode)) {
            HashMap<String, Object> prefs = new HashMap<String, Object>();
            prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus,
                PrefPasswordRecoveryAddressStatus.verified);
            prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, null);
            Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true,
                zsc.getAuthToken());

        } else {
            throw ServiceException
                .FAILURE("Verification of recovery email address verification code failed.", null);
        }
    }

    protected void resendCode(Account account, Mailbox mbox, ZimbraSoapContext zsc,
        OperationContext octxt, Channel channel) throws ServiceException {
        String verificationData = account.getRecoveryEmailVerificationData();
        if (verificationData == null) {
            throw ServiceException
            .FAILURE("The recovery email address verification data is missing.", null);
        }
        Map<String, String> dataMap = JWEUtil.getDecodedJWE(verificationData);
        String email = dataMap.get(CodeConstants.EMAIL.toString());
        String code = dataMap.get(CodeConstants.CODE.toString());
        long expiryTime = Long.parseLong(dataMap.get(CodeConstants.EXPIRY_TIME.toString()));
        int resendCount = Integer.parseInt(dataMap.get(CodeConstants.RESEND_COUNT.toString()));
        if (ZimbraLog.passwordreset.isDebugEnabled()) {
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String gmtDate = format.format(expiryTime);
            ZimbraLog.passwordreset.debug("resendCode: Current resend count: %d", resendCount);
            ZimbraLog.passwordreset.debug("resendCode: Resending code to: %s", email);
            ZimbraLog.passwordreset.debug("resendCode: Code expiry time: %s", gmtDate);
            ZimbraLog.passwordreset.debug("resendCode: Last 3 characters of resent code: %s",
                code.substring(5));
        }
        if (resendCount < account.getPasswordRecoveryMaxAttempts()) {
            // check if code is expired
            Date now = new Date();
            if (expiryTime < now.getTime()) {
                // generate new code and send
                sendCode(email, resendCount + 1, account, mbox, zsc, octxt, channel);
            } else {
                // send existing code
                Account authAccount = getAuthenticatedAccount(zsc);
                // update resend count
                resendCount = resendCount + 1;
                HashMap<String, Object> prefs = new HashMap<String, Object>();
                dataMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
                prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, JWEUtil.getJWE(dataMap));
                Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());
                SendRecoveryCode sendRecoveryCode = null;
                switch (channel) {
                    case EMAIL:
                        sendRecoveryCode = new EmailRecoveryCode(dataMap, mbox, authAccount);
                        break;
                    default:
                        throw ServiceException.INVALID_REQUEST("Invalid channel received.", null);
                }
                sendRecoveryCode.sendRecoveryAccountValidationCode(account, octxt);
            }
        } else {
            throw ServiceException.FAILURE("Resend code request has reached maximum limit.", null);
        }
    }

    protected void reset(Mailbox mbox, ZimbraSoapContext zsc) throws ServiceException {
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, null);
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus, null);
        prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, null);
        Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());

    }

    private static void validateEmail(String email, Account account) throws ServiceException {
        String accountName = account.getName();
        java.util.List<String> aliases = Arrays.asList(account.getAliases());
        if (ZimbraLog.passwordreset.isDebugEnabled()) {
            ZimbraLog.passwordreset.debug("validateEmail: Primary account name: %s", accountName);
            ZimbraLog.passwordreset.debug("validateEmail: Primary account aliases: %s", aliases);
        }
        if (aliases.contains(email) || accountName.equals(email)) {
            throw ServiceException
                .FAILURE("Recovery address should not be same as primary/alias email address.", null);
        }
    }
}
