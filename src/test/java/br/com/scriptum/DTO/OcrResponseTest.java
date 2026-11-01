package br.com.scriptum.DTO;

import br.com.scriptum.DTO.response.OcrResponse;
import br.com.scriptum.configuracao.UnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@UnitTest
class OcrResponseTest {

    @Test
    void trocaListaNulaPorListaVazia() {
        OcrResponse response = new OcrResponse(1L, null, 0);

        assertEquals(List.of(), response.paragraphs());
    }

    @Test
    void rejeitaConfiancaForaDoIntervaloPermitido() {
        assertThrows(IllegalArgumentException.class, () -> new OcrResponse(1L, List.of(), -1));
        assertThrows(IllegalArgumentException.class, () -> new OcrResponse(1L, List.of(), 101));
        assertThrows(IllegalArgumentException.class,
                () -> new OcrResponse(1L, List.of(), Double.NaN));
    }

    @Test
    void fazCopiaImutavelDaListaDeParagrafos() {
        var paragraphs = new java.util.ArrayList<>(List.of("texto"));
        OcrResponse response = new OcrResponse(1L, paragraphs, 90);
        paragraphs.add("alterado");

        assertEquals(List.of("texto"), response.paragraphs());
        assertThrows(UnsupportedOperationException.class, () -> response.paragraphs().add("novo"));
    }
}
