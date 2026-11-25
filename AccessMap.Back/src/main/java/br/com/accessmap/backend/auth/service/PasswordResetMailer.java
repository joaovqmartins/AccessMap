package br.com.accessmap.backend.auth.service;

import br.com.accessmap.backend.auth.config.PasswordResetProperties;
import br.com.accessmap.backend.auth.event.PasswordResetRequestedEvent;
import br.com.accessmap.backend.notification.MailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Manda o código depois do commit e fora da requisição: a resposta do pedido não espera o provedor,
 * e o tempo dela não revela se o e-mail tem conta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailer {

    private final MailSender mailSender;
    private final PasswordResetProperties props;

    @Async
    @TransactionalEventListener
    public void onResetRequested(PasswordResetRequestedEvent event) {
        String text = """
                Olá, %s.

                Seu código para criar uma nova senha no AccessMap é: %s

                Ele vale por %d minutos. Se não foi você quem pediu, ignore este e-mail: sua senha continua a mesma.
                """.formatted(event.name(), event.code(), props.codeTtl().toMinutes());
        try {
            mailSender.send(event.email(), "Código para redefinir sua senha no AccessMap", text);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar o código de recuperação de senha", e);
        }
    }
}
