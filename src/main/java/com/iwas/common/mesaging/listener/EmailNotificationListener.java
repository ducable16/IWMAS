package com.iwas.common.mesaging.listener;

import com.iwas.auth.entity.EmailMessage;
import com.iwas.config.RabbitMQConfig;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from:no-reply@roamtrip.local}")
    private String fromAddress;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consume(EmailMessage message) {
        try {
            Context context = new Context();
            if ("email-otp".equals(message.getTemplate())) {
                context.setVariable("otp", message.getToken());
            } else {
                String path = "email-verification".equals(message.getTemplate())
                        ? "/verify-email?token=" + message.getToken()
                        : "/reset-password?token=" + message.getToken();
                context.setVariable("actionUrl", frontendBaseUrl + path);
            }

            String htmlBody = templateEngine.process(message.getTemplate(), context);
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setText(htmlBody, true);
            mailSender.send(mime);
        } catch (Exception ignored) {
            // Keep consumer resilient in early-stage local setups.
        }
    }
}
