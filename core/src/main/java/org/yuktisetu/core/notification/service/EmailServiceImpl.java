package org.yuktisetu.core.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.yuktisetu.core.exception.EmailDeliveryException;
import org.yuktisetu.core.notification.config.EmailProperties;
import org.yuktisetu.core.notification.model.EmailRequest;
import org.yuktisetu.core.notification.model.InviteRecipient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailProperties emailProperties;

    public EmailServiceImpl(JavaMailSender mailSender,
                            SpringTemplateEngine templateEngine,
                            EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailProperties = emailProperties;
    }

    @Override
    public void sendEmail(EmailRequest request) {
        MimeMessage mimeMessage = buildMimeMessage(request);
        try {
            mailSender.send(mimeMessage);
            log.info("Sent email subject='{}' recipientCount={}", request.getSubject(), request.getTo().size());
        } catch (Exception e) {
            log.error("Failed to send email subject='{}' recipientCount={}: {}",
                    request.getSubject(), request.getTo().size(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void sendInviteEmail(List<String> recipients, String inviteeName, String inviteToken) {
        sendEmail(buildInviteRequest(recipients, inviteeName, inviteToken));
    }

    @Override
    public void sendBulkInviteEmails(List<InviteRecipient> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            log.warn("sendBulkInviteEmails called with no recipients, skipping");
            return;
        }

        List<MimeMessage> messages = new ArrayList<>();

        for (InviteRecipient recipient : recipients) {
            EmailRequest request = buildInviteRequest(
                    List.of(recipient.getEmail()),
                    recipient.getName(),
                    recipient.getToken()
            );
            messages.add(buildMimeMessage(request));
        }

        try {
            mailSender.send(messages.toArray(new MimeMessage[0]));
            log.info("Sent bulk invite emails count={}", messages.size());
        } catch (Exception e) {
            log.error("Failed to send bulk invite emails count={}: {}", messages.size(), e.getMessage());
            throw e;
        }
    }

    private EmailRequest buildInviteRequest(List<String> recipients, String inviteeName, String inviteToken) {
        String inviteLink = emailProperties.getBaseInviteUrl() + "?token=" + inviteToken;

        return EmailRequest.builder()
                .to(recipients)
                .subject("You're invited — complete your profile setup")
                .templateName("invite-token-email")
                .templateVariables(Map.of(
                        "inviteeName", inviteeName,
                        "inviteLink", inviteLink
                ))
                .build();
    }

    private MimeMessage buildMimeMessage(EmailRequest request) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8"
            );

            helper.setFrom(emailProperties.getDefaultFrom(), emailProperties.getDefaultFromName());
            helper.setTo(request.getTo().toArray(new String[0]));

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            List<String> bcc = request.getBcc() != null ? request.getBcc() : emailProperties.getDefaultBcc();
            if (bcc != null && !bcc.isEmpty()) {
                helper.setBcc(bcc.toArray(new String[0]));
            }

            helper.setSubject(request.getSubject());

            Context context = new Context();
            if (request.getTemplateVariables() != null) {
                for (Map.Entry<String, Object> entry : request.getTemplateVariables().entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            }

            String htmlBody = templateEngine.process(request.getTemplateName(), context);
            helper.setText(htmlBody, true);

            return mimeMessage;
        } catch (Exception e) {
            log.error("Failed to build email recipientCount={}: {}", request.getTo().size(), e.getMessage());
            throw new EmailDeliveryException("Failed to build email for " + request.getTo(), e);
        }
    }
}