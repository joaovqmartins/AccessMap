package br.com.accessmap.backend.infrastructure.user;

import br.com.accessmap.backend.domain.user.entity.User;
import br.com.accessmap.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> store = new LinkedHashMap<>();

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public User save(User user) {
        store.put(user.getId(), user);
        return user;
    }

    @Override
    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.values().stream()
                .anyMatch(u -> u.getEmail().equals(email));
    }

    @Override
    public boolean existsByEmailExcludingId(String email, String excludeId) {
        return store.values().stream()
                .anyMatch(u -> u.getEmail().equals(email) && !u.getId().equals(excludeId));
    }
}
