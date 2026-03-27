package com.bank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Async
    public void sendTransactionAlert(String toEmail, String fullName, String txRef,
                                     String type, BigDecimal amount, BigDecimal newBalance, String currency) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Skipping email to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@securebank.com");
            message.setTo(toEmail);
            message.setSubject("SecureBank Alert: " + type + " of " + currency + " " + amount);
            message.setText(
                "Dear " + fullName + ",\n\n" +
                "A " + type.toLowerCase() + " has been processed on your account.\n\n" +
                "Transaction Reference : " + txRef + "\n" +
                "Type                  : " + type + "\n" +
                "Amount                : " + currency + " " + amount + "\n" +
                "New Balance           : " + currency + " " + newBalance + "\n\n" +
                "If you did not authorize this transaction, please contact us immediately at support@securebank.com\n\n" +
                "Regards,\nSecureBank Team"
            );
            mailSender.send(message);
            log.info("Transaction alert sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName, String username) {
        if (mailSender == null) {
            log.warn("Mail sender not configured. Skipping welcome email to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@securebank.com");
            message.setTo(toEmail);
            message.setSubject("Welcome to SecureBank!");
            message.setText(
                "Dear " + fullName + ",\n\n" +
                "Welcome to SecureBank! Your account has been created successfully.\n\n" +
                "Username: " + username + "\n\n" +
                "You can now log in and create bank accounts to start transacting.\n\n" +
                "Regards,\nSecureBank Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendLoginAlert(String toEmail, String fullName, String ipAddress) {
        if (mailSender == null) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@securebank.com");
            message.setTo(toEmail);
            message.setSubject("SecureBank: New Login Detected");
            message.setText(
                "Dear " + fullName + ",\n\n" +
                "A new login was detected on your account.\n\n" +
                "IP Address : " + ipAddress + "\n" +
                "Time       : " + java.time.LocalDateTime.now() + "\n\n" +
                "If this wasn't you, please change your password immediately.\n\n" +
                "Regards,\nSecureBank Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send login alert to {}: {}", toEmail, e.getMessage());
        }
    }
}
