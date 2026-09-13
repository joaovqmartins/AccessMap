package br.com.accessmap.backend.auth.service;

import br.com.accessmap.backend.auth.dto.LoginRequestDto;
import br.com.accessmap.backend.auth.dto.RegisterRequestDto;
import br.com.accessmap.backend.auth.dto.TokenResponseDto;
import br.com.accessmap.backend.identity.dto.UserRequestDto;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.identity.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Hash de uma senha que ninguém tem. Quando o e-mail não existe, comparamos contra ele
     * mesmo assim, para que login com e-mail inexistente demore o mesmo que login com senha
     * errada — sem isso, o tempo de resposta revela quais e-mails estão cadastrados.
     */
    private final String dummyHash;

    public AuthService(UserService userService,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyHash = passwordEncoder.encode("senha-inexistente-para-tempo-constante");
    }

    @Transactional
    public TokenResponseDto register(RegisterRequestDto request) {
        UserRequestDto userRequest = new UserRequestDto();
        userRequest.setName(request.getName());
        userRequest.setEmail(request.getEmail());
        userRequest.setPassword(request.getPassword());
        userRequest.setPhone(request.getPhone());
        userRequest.setAge(request.getAge());
        userRequest.setAccessibilityNeeds(request.getAccessibilityNeeds());

        User created = userService.create(userRequest);
        return issueTokens(created);
    }

    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        String storedHash = user != null ? user.getPassword() : dummyHash;
        boolean matches = passwordEncoder.matches(request.getPassword(), storedHash);

        if (user == null || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenResponseDto refresh(String refreshToken) {
        String userId = refreshTokenService.rotate(refreshToken);
        User user = userService.findById(userId);
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private TokenResponseDto issueTokens(User user) {
        return TokenResponseDto.builder()
                .accessToken(tokenService.issueAccessToken(user))
                .refreshToken(refreshTokenService.issue(user.getId()))
                .tokenType(TOKEN_TYPE)
                .expiresIn(tokenService.accessTokenTtlSeconds())
                .user(user)
                .build();
    }
}
