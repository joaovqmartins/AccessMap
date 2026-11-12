package br.com.accessmap.backend.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxBytesValidatorTest {

    private final MaxBytesValidator validator = new MaxBytesValidator();

    @MaxBytes(4)
    private String campo;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        validator.initialize(getClass().getDeclaredField("campo").getAnnotation(MaxBytes.class));
    }

    @Test
    void deveAceitarNulo() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void deveAceitarTextoNoLimite() {
        assertTrue(validator.isValid("abcd", null));
    }

    @Test
    void deveRecusarTextoAcimaDoLimite() {
        assertFalse(validator.isValid("abcde", null));
    }

    @Test
    void deveContarAcentoComoDoisBytes() {
        assertTrue(validator.isValid("éé", null));
        assertFalse(validator.isValid("ééa", null));
    }
}
