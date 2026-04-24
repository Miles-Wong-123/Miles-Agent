package com.miles.milesagent.tool;

import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 提供给模型的邮件发送工具。
 * 当用户明确要求“发邮件”时，模型可以组装参数后调用这里。
 */
@Component
@Slf4j
public class EmailTool {

    /**
     * Spring 提供的邮件发送器。
     */
    @Resource
    private JavaMailSender mailSender;

    /**
     * 发件邮箱地址，来自配置文件，避免硬编码。
     */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送简单文本邮件。
     *
     * @param targetEmail 收件人邮箱
     * @param subject     邮件标题
     * @param content     邮件正文
     */
    @Tool("向特定用户发送电子邮件。")
    public String sendEmail(String targetEmail, String subject, String content) {
        try {
            log.info("Tool 调用: 正在发送邮件 -> To: {}, Subject: {}", targetEmail, subject);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(targetEmail);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            
            log.info("邮件发送成功");
            return "邮件已成功发送给 " + targetEmail;
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            return "邮件发送失败: " + e.getMessage();
        }
    }
}
