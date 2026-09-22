package br.com.accessmap.backend.domain.user.repository;

import br.com.accessmap.backend.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();
    Optional<User> findById(String id);
    User save(User user);
    boolean deleteById(String id);
    boolean existsByEmail(String email);
    boolean existsByEmailExcludingId(String email, String excludeId);
}
