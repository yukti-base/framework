package org.yuktisetu.core.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.yuktisetu.core.notification.service.EmailService;
import org.yuktisetu.core.notification.service.EmailServiceImpl;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties({EmailProperties.class, MailProperties.class})
@ConditionalOnProperty(prefix = "app.email", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailAutoConfiguration {

    @Bean
    public JavaMailSender javaMailSender(MailProperties mailProperties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailProperties.getHost());
        sender.setPort(mailProperties.getPort());
        sender.setUsername(mailProperties.getUsername());
        sender.setPassword(mailProperties.getPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        mailProperties.getProperties().forEach(props::put);

        return sender;
    }

    @Bean
    public ClassLoaderTemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("email/template/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setOrder(1);
        resolver.setCheckExistence(true);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine emailTemplateEngine(ClassLoaderTemplateResolver emailTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(emailTemplateResolver);
        return engine;
    }

    @Bean
    public EmailService emailService(JavaMailSender javaMailSender,
                                     SpringTemplateEngine emailTemplateEngine,
                                     EmailProperties emailProperties) {
        return new EmailServiceImpl(javaMailSender, emailTemplateEngine, emailProperties);
    }
}