package br.com.accessmap.backend.auth.dto;

import br.com.accessmap.backend.identity.model.User;
import lombok.Builder;

@Builder
public record TokenResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        User user
) {
}
