package br.com.accessmap.backend.auth.model;

import br.com.accessmap.backend.auth.enums.AuthAttemptKind;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auth_attempts")
public class AuthAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthAttemptKind kind;

    @Column(nullable = false, length = 64)
    private String keyHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
