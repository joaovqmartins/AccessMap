package br.com.accessmap.backend.auth;

import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Cria o primeiro ADMIN a partir de ADMIN_EMAIL / ADMIN_PASSWORD, se ambos estiverem definidos
 * e ainda não existir nenhum ADMIN. Evita hash fixo em migração e senha padrão conhecida.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.warn("ADMIN_EMAIL {} já está cadastrado como usuário comum; nenhum ADMIN foi criado", adminEmail);
            return;
        }

        userRepository.save(User.builder()
                .name("Administrador")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .accessibilityNeeds(Set.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        log.info("Usuário ADMIN inicial criado: {}", adminEmail);
    }
}
