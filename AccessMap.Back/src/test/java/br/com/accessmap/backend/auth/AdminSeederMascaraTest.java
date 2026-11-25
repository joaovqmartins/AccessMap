package br.com.accessmap.backend.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** O telefone do ADMIN não aparece inteiro no log (#132). */
class AdminSeederMascaraTest {

    @Test
    void mostraSoOsQuatroUltimosDigitos() {
        assertThat(AdminSeeder.mascarar("11987654321")).isEqualTo("***4321");
    }

    @Test
    void telefoneCurtoFicaTodoEscondido() {
        assertThat(AdminSeeder.mascarar("123")).isEqualTo("****");
    }
}
