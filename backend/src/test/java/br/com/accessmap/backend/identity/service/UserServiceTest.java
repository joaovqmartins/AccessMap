package br.com.accessmap.backend.identity.service;

import br.com.accessmap.backend.identity.dto.UserRequestDto;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserRequestDto validRequest() {
        UserRequestDto dto = new UserRequestDto();
        dto.setName("Maria");
        dto.setEmail("maria@email.com");
        dto.setPassword("senha1234");
        dto.setPhone("11999999999");
        dto.setAge(30);
        dto.setAccessibilityNeeds("Cadeirante");
        return dto;
    }

    @Test
    void deveCriarUsuarioComDadosValidos() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User criado = userService.create(validRequest());

        assertThat(criado.getName()).isEqualTo("Maria");
        assertThat(criado.getEmail()).isEqualTo("maria@email.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deveRejeitarCriacaoComCampoObrigatorioFaltando() {
        UserRequestDto dto = validRequest();
        dto.setName(null);

        assertThrows(ResponseStatusException.class, () -> userService.create(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deveRejeitarCriacaoComIdadeInvalida() {
        UserRequestDto dto = validRequest();
        dto.setAge(0);

        assertThrows(ResponseStatusException.class, () -> userService.create(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deveRejeitarCriacaoComEmailJaCadastrado() {
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> userService.create(validRequest()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deveLancarNotFoundQuandoUsuarioNaoExiste() {
        when(userRepository.findById("id-invalido")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> userService.findById("id-invalido"));
    }

    @Test
    void deveAtualizarApenasCamposInformados() {
        User existente = User.builder()
                .id("1")
                .name("Nome Antigo")
                .email("antigo@email.com")
                .phone("11988887777")
                .age(25)
                .accessibilityNeeds("Nenhuma")
                .password("senhaAntiga")
                .build();

        when(userRepository.findById("1")).thenReturn(Optional.of(existente));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRequestDto dto = new UserRequestDto();
        dto.setName("Nome Novo");

        User atualizado = userService.update("1", dto);

        assertThat(atualizado.getName()).isEqualTo("Nome Novo");
        assertThat(atualizado.getEmail()).isEqualTo("antigo@email.com");
    }

    @Test
    void deveRejeitarAtualizacaoParaEmailJaUsadoPorOutroUsuario() {
        User existente = User.builder().id("1").email("antigo@email.com").build();

        when(userRepository.findById("1")).thenReturn(Optional.of(existente));
        when(userRepository.existsByEmailExcludingId("novo@email.com", "1")).thenReturn(true);

        UserRequestDto dto = new UserRequestDto();
        dto.setEmail("novo@email.com");

        assertThrows(ResponseStatusException.class, () -> userService.update("1", dto));
    }

    @Test
    void deveRemoverUsuarioExistente() {
        User existente = User.builder().id("1").build();
        when(userRepository.findById("1")).thenReturn(Optional.of(existente));

        userService.delete("1");

        verify(userRepository).deleteById("1");
    }
}
