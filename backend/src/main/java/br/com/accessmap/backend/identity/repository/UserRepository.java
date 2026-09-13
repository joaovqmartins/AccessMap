package br.com.accessmap.backend.identity.repository;

import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    boolean existsByRole(Role role);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id <> :excludeId")
    boolean existsByEmailExcludingId(String email, String excludeId);
}
