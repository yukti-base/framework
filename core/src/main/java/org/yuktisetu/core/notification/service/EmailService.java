package org.yuktisetu.core.notification.service;

import org.yuktisetu.core.notification.model.EmailRequest;
import org.yuktisetu.core.notification.model.InviteRecipient;

import java.util.List;

public interface EmailService {

    void sendEmail(EmailRequest request);

    void sendBulkInviteEmails(List<InviteRecipient> recipients);

    void sendInviteEmail(java.util.List<String> recipients, String inviteeName, String inviteToken);
}