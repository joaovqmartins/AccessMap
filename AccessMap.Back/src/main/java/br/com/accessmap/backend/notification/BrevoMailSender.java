package br.com.accessmap.backend.notification;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** https://developers.brevo.com/reference/sendtransacemail */
public class BrevoMailSender implements MailSender {

    private static final String URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final MailProperties props;

    public BrevoMailSender(RestClient restClient, MailProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    @Override
    public void send(String to, String subject, String text) {
        restClient.post()
                .uri(URL)
                .header("api-key", props.brevoApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sender", Map.of("name", props.fromName(), "email", props.from()),
                        "to", List.of(Map.of("email", to)),
                        "subject", subject,
                        "textContent", text))
                .retrieve()
                .toBodilessEntity();
    }
}
