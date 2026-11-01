package br.com.scriptum.DTO;

import br.com.scriptum.DTO.request.OcrRequest;
import br.com.scriptum.configuracao.UnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@UnitTest
class OcrRequestTest {

    @Test
    void normalizaIdiomaParaMinusculas() {
        OcrRequest request = new OcrRequest(1L, "ENG", "base64");

        assertEquals("eng", request.language());
    }

    @Test
    void rejeitaIdentificadorNuloOuNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new OcrRequest(null, "eng", "base64"));
        assertThrows(IllegalArgumentException.class, () -> new OcrRequest(-1L, "eng", "base64"));
    }

    @Test
    void permiteIdiomaNuloParaValidacaoPosterior() {
        OcrRequest request = new OcrRequest(1L, null, "base64");

        assertNull(request.language());
    }
}
