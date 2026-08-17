package br.com.accessmap.backend.application.user;

import br.com.accessmap.backend.application.user.dto.UserRequest;
import br.com.accessmap.backend.domain.user.entity.User;
import br.com.accessmap.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public User create(UserRequest request) {
        validateRequiredFields(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já está em uso");
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .age(request.getAge())
                .accessibilityNeeds(request.getAccessibilityNeeds())
                .password(request.getPassword())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return userRepository.save(user);
    }

    public User update(String id, UserRequest request) {
        User existing = findById(id);

        if (request.getEmail() != null && userRepository.existsByEmailExcludingId(request.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já está em uso");
        }

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getEmail() != null) existing.setEmail(request.getEmail());
        if (request.getPhone() != null) existing.setPhone(request.getPhone());
        if (request.getAge() != null) existing.setAge(request.getAge());
        if (request.getAccessibilityNeeds() != null) existing.setAccessibilityNeeds(request.getAccessibilityNeeds());
        if (request.getPassword() != null) existing.setPassword(request.getPassword());
        existing.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return userRepository.save(existing);
    }

    public void delete(String id) {
        findById(id);
        userRepository.deleteById(id);
    }

    private void validateRequiredFields(UserRequest request) {
        if (isBlank(request.getName()) ||
                isBlank(request.getEmail()) ||
                isBlank(request.getPhone()) ||
                request.getAge() == null ||
                isBlank(request.getAccessibilityNeeds()) ||
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
}
