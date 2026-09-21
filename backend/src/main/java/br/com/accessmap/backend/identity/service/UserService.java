package br.com.accessmap.backend.identity.service;

import br.com.accessmap.backend.identity.dto.UserRequestDto;
import br.com.accessmap.backend.identity.event.PasswordChangedEvent;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public User create(UserRequestDto request) {
        validateRequiredFields(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já está em uso");
        }


        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .age(request.getAge())
                .accessibilityNeeds(request.getAccessibilityNeeds())
                .password(passwordEncoder.encode(request.getPassword()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User update(String id, UserRequestDto request) {
        User existing = findById(id);
        validateUpdatableFields(request);

        if (request.getEmail() != null && userRepository.existsByEmailExcludingId(request.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já está em uso");
        }

        boolean passwordChanged = request.getPassword() != null;
        if (passwordChanged && (request.getCurrentPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), existing.getPassword()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        }

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getEmail() != null) existing.setEmail(request.getEmail());
        if (request.getPhone() != null) existing.setPhone(request.getPhone());
        if (request.getAge() != null) existing.setAge(request.getAge());
        if (request.getAccessibilityNeeds() != null) existing.setAccessibilityNeeds(request.getAccessibilityNeeds());
        if (passwordChanged) existing.setPassword(passwordEncoder.encode(request.getPassword()));
        existing.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(existing);

        if (passwordChanged) {
            eventPublisher.publishEvent(new PasswordChangedEvent(saved.getId()));
        }

        return saved;
    }

    public void delete(String id) {
        findById(id);
        userRepository.deleteById(id);
    }

    /**
     * No PATCH, um campo só pode ser omitido (null, mantém o valor atual) ou informado com
     * conteúdo válido — nunca esvaziado. O DTO é compartilhado com a criação, onde os campos
     * são obrigatórios, então essa checagem não pode virar {@code @NotBlank} nele: quebraria
     * atualizações parciais legítimas que omitem outros campos.
     */
    private void validateUpdatableFields(UserRequestDto request) {
        if (request.getName() != null && isBlank(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name não pode ser vazio");
        }
        if (request.getEmail() != null && isBlank(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email não pode ser vazio");
        }
        if (request.getPhone() != null && isBlank(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone não pode ser vazio");
        }
        if (request.getAccessibilityNeeds() != null && isEmpty(request.getAccessibilityNeeds())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accessibilityNeeds não pode ser vazio");
        }
    }

    private void validateRequiredFields(UserRequestDto request) {
        if (isBlank(request.getName()) ||
                isBlank(request.getEmail()) ||
                isBlank(request.getPhone()) ||
                request.getAge() == null ||
                isEmpty(request.getAccessibilityNeeds()) ||
                isBlank(request.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Campos obrigatórios: name, email, phone, age, accessibilityNeeds, password"
            );
        }
        if (request.getAge() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "age deve ser um número positivo");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isEmpty(Collection<?> value) {
        return value == null || value.isEmpty();
    }
}
