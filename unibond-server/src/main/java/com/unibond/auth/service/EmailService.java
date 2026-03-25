package com.unibond.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                        @Value("${spring.mail.username:noreply@unibond.app}") String fromEmail) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
    }

    public void sendVerificationCode(String to, String code) throws MessagingException {
        Context ctx = new Context();
        ctx.setVariable("code", code);
        String html = templateEngine.process("verification-code", ctx);

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("UniBond 验证码");
        helper.setText(html, true);
        mailSender.send(msg);
    }
}
