package br.com.accessmap.backend.notification;

import lombok.extern.slf4j.Slf4j;

/** Usado quando não há chave do Brevo: no desenvolvimento local, o código aparece no log da API. */
@Slf4j
public class LogMailSender implements MailSender {

    private final boolean logContent;

    public LogMailSender(boolean logContent) {
        this.logContent = logContent;
    }

    @Override
    public void send(String to, String subject, String text) {
        if (logContent) {
            log.info("E-mail NÃO enviado (sem BREVO_API_KEY). Para: {} | Assunto: {}\n{}", to, subject, text);
        } else {
            log.warn("E-mail NÃO enviado: configure BREVO_API_KEY. Assunto: {}", subject);
        }
    }
}
