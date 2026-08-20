package br.com.accessmap.backend.shared.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationValidatorTest {

    @Test
    void deveAceitarCoordenadasValidas() {

        double latitude = -22.9068;
        double longitude = -43.1729;

        boolean resultado = LocationValidator.isValid(latitude, longitude);

        assertTrue(resultado);
    }

    @Test
void deveAceitarLatitudeMinima() {
    assertTrue(LocationValidator.isValid(-90, 0));
}

@Test
void deveAceitarLatitudeMaxima() {
    assertTrue(LocationValidator.isValid(90, 0));
}

@Test
void deveAceitarLongitudeMinima() {
    assertTrue(LocationValidator.isValid(0, -180));
}

@Test
void deveAceitarLongitudeMaxima() {
    assertTrue(LocationValidator.isValid(0, 180));
}

@Test
void deveRejeitarLatitudeMaiorQue90() {
    assertFalse(LocationValidator.isValid(90.1, 0));
}

@Test
void deveRejeitarLatitudeMenorQueMenos90() {
    assertFalse(LocationValidator.isValid(-90.1, 0));
}

@Test
void deveRejeitarLongitudeMaiorQue180() {
    assertFalse(LocationValidator.isValid(0, 180.1));
}

@Test
void deveRejeitarLongitudeMenorQueMenos180() {
    assertFalse(LocationValidator.isValid(0, -180.1));
}
}