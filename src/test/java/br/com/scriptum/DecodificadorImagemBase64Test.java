package br.com.scriptum;

import br.com.scriptum.service.decodificaImagem.DecodificadorImagemBase64;
import br.com.scriptum.configuracao.UnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@UnitTest
class DecodificadorImagemBase64Test {

    private final DecodificadorImagemBase64 decodificador = new DecodificadorImagemBase64();

    @Test
    void rejeitaBase64Invalido() {
        assertThrows(IllegalArgumentException.class,
                () -> decodificador.decodificar("imagem-invalida"));
    }

    @Test
    void rejeitaBase64QueNaoRepresentaImagem() {
        assertThrows(IllegalArgumentException.class,
                () -> decodificador.decodificar("aA=="));
    }
}
