package org.yuktisetu.core.notification.config;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The one JavaMailSender in the fleet, and the one place a BCC is guaranteed.
 *
 * Every mail any service sends -- the core EmailService (invites, activation),
 * notification-service's delivery drain (every drive, receipt and staff notice),
 * and anything written later -- ends up in doSend() below, so adding the address
 * here cannot be bypassed by a code path, a per-request bcc list, or a missing
 * property in some service's config. It used to be a per-service
 * app.email.default-bcc setting: commented out in auth-service, absent in
 * notification-service (which sends the bulk of all mail), and replaced outright
 * whenever a request carried its own bcc list.
 *
 * Sending to the audit address must never be the reason a real mail fails, so a
 * problem adding it is logged and the mail goes out regardless.
 */
@Slf4j
public class AlwaysBccMailSender extends JavaMailSenderImpl {

    private final Collection<String> alwaysBcc;

    public AlwaysBccMailSender(Collection<String> alwaysBcc) {
        this.alwaysBcc = alwaysBcc == null ? Set.of() : alwaysBcc;
    }

    @Override
    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) throws org.springframework.mail.MailException {
        for (MimeMessage message : mimeMessages) {
            addBcc(message, alwaysBcc);
        }
        super.doSend(mimeMessages, originalMessages);
    }

    /**
     * Adds each address as a BCC recipient, skipping any that is already a To, Cc
     * or Bcc recipient of this message (so someone who is both the recipient and the
     * audit address gets one copy, not two). Case-insensitive on the address.
     */
    public static void addBcc(MimeMessage message, Collection<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        try {
            Set<String> already = new HashSet<>();
            Address[] existing = message.getAllRecipients();
            if (existing != null) {
                for (Address a : existing) {
                    already.add(normalise(a instanceof InternetAddress ia ? ia.getAddress() : a.toString()));
                }
            }
            for (String raw : addresses) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String address = raw.trim();
                if (already.add(normalise(address))) {
                    message.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
                }
            }
        } catch (Exception e) {
            log.error("Could not add the always-BCC recipient(s) {}; sending without them: {}",
                    addresses, e.getMessage());
        }
    }

    private static String normalise(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }
}
