package br.com.accessmap.backend.auth.service;

import br.com.accessmap.backend.auth.config.PasswordResetProperties;
import br.com.accessmap.backend.auth.event.PasswordResetRequestedEvent;
import br.com.accessmap.backend.auth.model.PasswordResetCode;
import br.com.accessmap.backend.auth.repository.PasswordResetCodeRepository;
import br.com.accessmap.backend.identity.event.PasswordChangedEvent;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Recuperação de senha por código enviado ao e-mail (#109). Nenhuma resposta revela se o e-mail
 * tem conta: o pedido responde sempre igual e a confirmação falha sempre com a mesma mensagem.
 */
@Service
public class PasswordResetService {

    public static final String CODIGO_INVALIDO = "Código inválido ou expirado";

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAttemptService authAttemptService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordResetProperties props;
    private final SecureRandom random = new SecureRandom();

    /** Mesmo papel do dummyHash do {@link AuthService}: e-mail sem conta custa o mesmo BCrypt. */
    private final String dummyHash;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetCodeRepository codeRepository,
                                PasswordEncoder passwordEncoder,
                                AuthAttemptService authAttemptService,
                                ApplicationEventPublisher eventPublisher,
                                PasswordResetProperties props) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.passwordEncoder = passwordEncoder;
        this.authAttemptService = authAttemptService;
        this.eventPublisher = eventPublisher;
        this.props = props;
        this.dummyHash = passwordEncoder.encode("codigo-inexistente");
    }

    @Transactional
    public void request(String rawEmail, String ip) {
        String email = normalize(rawEmail);
        authAttemptService.registerResetRequest(email, ip);

        // Gera e faz o hash antes de procurar a conta: o tempo de resposta é o mesmo nos dois casos
        String code = "%06d".formatted(random.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);

        Optional<User> found = userRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();

        LocalDateTime now = LocalDateTime.now();
        codeRepository.invalidateActiveByUserId(user.getId(), now);
        codeRepository.save(PasswordResetCode.builder()
                .userId(user.getId())
                .codeHash(codeHash)
                .attempts(0)
                .expiresAt(now.plus(props.codeTtl()))
                .createdAt(now)
                .build());
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(email, user.getName(), code));
    }

    /**
     * Não desfaz a transação no 400: a tentativa errada precisa ficar gravada, senão o limite
     * de tentativas nunca seria alcançado.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void confirm(String rawEmail, String code, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(normalize(rawEmail)).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode active = user == null ? null : codeRepository.findActive(user.getId(), now).orElse(null);

        if (active == null) {
            passwordEncoder.matches(code, dummyHash);
            throw invalidCode();
        }

        if (!passwordEncoder.matches(code, active.getCodeHash())) {
            active.setAttempts(active.getAttempts() + 1);
            if (active.getAttempts() >= props.maxAttempts()) {
                active.setUsedAt(now);
            }
            codeRepository.save(active);
            throw invalidCode();
        }

        active.setUsedAt(now);
        codeRepository.save(active);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(now);
        userRepository.save(user);
        // Quem esqueceu a senha não deve continuar bloqueado pelos logins errados de antes
        authAttemptService.clearLoginFailures(user.getPhone());
        // Encerra todas as sessões, como na troca de senha pelo perfil
        eventPublisher.publishEvent(new PasswordChangedEvent(user.getId()));
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, CODIGO_INVALIDO);
    }
}
