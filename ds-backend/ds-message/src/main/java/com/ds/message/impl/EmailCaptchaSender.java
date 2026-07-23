package com.ds.message.impl;

import cn.hutool.core.util.StrUtil;
import com.ds.message.config.MessageProperties;
import com.ds.message.core.CaptchaScene;
import com.ds.message.core.CaptchaSender;
import com.ds.message.core.CaptchaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailCaptchaSender implements CaptchaSender {

    private final MessageProperties messageProperties;

    @Override
    public boolean supports(CaptchaType type) {
        return type == CaptchaType.EMAIL;
    }

    @Override
    public void send(String target, String code, CaptchaScene scene) {
        if (!messageProperties.getEmailEnabled()) {
            log.warn("邮件验证码功能未启用");
            return;
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(messageProperties.getEmailHost());
        mailSender.setPort(messageProperties.getEmailPort());
        mailSender.setUsername(messageProperties.getEmailUsername());
        mailSender.setPassword(messageProperties.getEmailPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        SimpleMailMessage message = new SimpleMailMessage();
        String fromName = messageProperties.getEmailFromName();
        String fromEmail = messageProperties.getEmailUsername();
        if (StrUtil.isNotBlank(fromName) && !fromName.equals(fromEmail)) {
            message.setFrom(fromName + " <" + fromEmail + ">");
        } else {
            message.setFrom(fromEmail);
        }
        message.setTo(target);
        message.setSubject("验证码");
        message.setText("您的验证码是：" + code + "，5分钟内有效");

        mailSender.send(message);
        log.info("邮件验证码已发送至：{}", target);
    }
}
