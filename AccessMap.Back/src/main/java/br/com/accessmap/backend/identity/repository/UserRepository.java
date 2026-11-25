package br.com.accessmap.backend.identity.repository;

import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.phone = :phone AND u.id <> :excludeId")
    boolean existsByPhoneExcludingId(String phone, String excludeId);

    /** O e-mail é gravado em minúsculas (V14), mas a busca ignora a caixa mesmo assim. */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE lower(u.email) = lower(:email) AND u.id <> :excludeId")
    boolean existsByEmailExcludingId(String email, String excludeId);

    boolean existsByRole(Role role);
}
