package br.com.accessmap.backend.infrastructure.user;

import br.com.accessmap.backend.domain.user.entity.User;
import br.com.accessmap.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpa;

    @Override
    public List<User> findAll() {
        return jpa.findAll();
    }

    @Override
    public Optional<User> findById(String id) {
        return jpa.findById(id);
    }

    @Override
    public User save(User user) {
        return jpa.save(user);
    }

    @Override
    public boolean deleteById(String id) {
        if (!jpa.existsById(id)) return false;
        jpa.deleteById(id);
        return true;
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailExcludingId(String email, String excludeId) {
        return jpa.existsByEmailExcludingId(email, excludeId);
    }
}