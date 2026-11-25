package br.com.accessmap.backend.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    public MailSender mailSender(MailProperties props) {
        if (props.enabled()) {
            return new BrevoMailSender(RestClient.create(), props);
        }
        log.warn("BREVO_API_KEY não configurada: os e-mails não serão enviados, só registrados no log.");
        return new LogMailSender(props.logContent());
    }
}
