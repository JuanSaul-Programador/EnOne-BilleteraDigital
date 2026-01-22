package com.enone.util.email;

public interface EmailService {
    void send(String to, String subject, String htmlBody);
    void sendText(String to, String subject, String textBody);
    boolean isServiceAvailable();
}