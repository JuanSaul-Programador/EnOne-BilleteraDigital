package com.enone.util.email;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String senderName;

    public EmailServiceImpl(JavaMailSender mailSender,
                           @Value("${app.email.sender.email:enoneproyecto@gmail.com}") String senderEmail,
                           @Value("${app.email.sender.name:EnOne}") String senderName) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("Enviando email HTML a: {}", to);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            try {
                helper.setFrom(senderEmail, senderName);
            } catch (UnsupportedEncodingException e) {
                log.warn("Error con encoding del nombre, usando solo email: {}", e.getMessage());
                helper.setFrom(senderEmail);
            }

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("Email HTML enviado exitosamente a: {}", to);
            
        } catch (MessagingException e) {
            log.error("Error enviando email HTML a: {}", to, e);
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendText(String to, String subject, String textBody) {
        log.info("Enviando email de texto a: {}", to);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(textBody);
            
            mailSender.send(message);
            log.info("Email de texto enviado exitosamente a: {}", to);
            
        } catch (Exception e) {
            log.error("Error enviando email de texto a: {}", to, e);
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            // Intenta crear un mensaje para verificar la configuración
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.warn("Servicio de email no disponible: {}", e.getMessage());
            return false;
        }
    }
}