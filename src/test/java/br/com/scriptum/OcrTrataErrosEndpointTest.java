package br.com.scriptum;

import br.com.scriptum.DTO.request.OcrRequest;
import br.com.scriptum.DTO.response.OcrResponse;
import br.com.scriptum.configuracao.UnitTest;
import br.com.scriptum.messaging.OcrResultPublisher;
import br.com.scriptum.resource.OcrResource;
import br.com.scriptum.service.OcrService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class OcrTrataErrosEndpointTest {

    @Mock
    private OcrService ocrService;

    @Mock
    private OcrResultPublisher resultPublisher;

    @InjectMocks
    private OcrResource resource;

    @Test
    void retornaBadRequestQuandoOProcessamentoRecebeEntradaInvalida() {
        OcrRequest requisicao = new OcrRequest(10L, "por", "aA==");
        when(ocrService.processar(requisicao))
                .thenThrow(new IllegalArgumentException("imagem inválida"));

        var resposta = resource.processar(requisicao);

        assertEquals(400, resposta.getStatus());
        assertEquals("imagem inválida", resposta.getEntity());
    }

    @Test
    void propagaFalhaQuandoNaoConseguePublicarResposta() {
        OcrRequest requisicao = new OcrRequest(10L, "por", "aA==");
        OcrResponse resposta = new OcrResponse(10L, List.of("texto"), 90);
        when(ocrService.processar(requisicao)).thenReturn(resposta);
        doThrow(new IllegalStateException("falha ao publicar"))
                .when(resultPublisher).publicar(resposta);

        assertThrows(IllegalStateException.class, () -> resource.processar(requisicao));
        verify(resultPublisher).publicar(resposta);
    }
}
