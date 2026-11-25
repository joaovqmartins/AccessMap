package br.com.accessmap.backend.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Envio de e-mail pela API HTTP do Brevo. O Railway bloqueia SMTP nos planos Free e Hobby, por isso
 * não usamos o spring-boot-starter-mail.
 *
 * @param brevoApiKey sem chave, nada é enviado: o {@link LogMailSender} só registra no log
 * @param logContent  se o log do {@link LogMailSender} mostra o texto (com o código). Desligado em produção.
 */
@ConfigurationProperties(prefix = "accessmap.mail")
public record MailProperties(String brevoApiKey, String from, String fromName, boolean logContent) {

    public boolean enabled() {
        return brevoApiKey != null && !brevoApiKey.isBlank();
    }
}
