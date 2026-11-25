package br.com.accessmap.backend.notification;

/** Envia e-mail de texto simples. A implementação é escolhida no {@link MailConfig}. */
public interface MailSender {

    void send(String to, String subject, String text);
}
