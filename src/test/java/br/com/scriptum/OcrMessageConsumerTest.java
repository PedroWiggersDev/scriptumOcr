package br.com.scriptum;

import br.com.scriptum.DTO.request.OcrRequest;
import br.com.scriptum.DTO.response.OcrResponse;
import br.com.scriptum.configuracao.UnitTest;
import br.com.scriptum.messaging.OcrMessageConsumer;
import br.com.scriptum.messaging.OcrResultPublisher;
import br.com.scriptum.service.OcrService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class OcrMessageConsumerTest {

    @Mock
    private OcrService ocrService;

    @Mock
    private OcrResultPublisher resultPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OcrMessageConsumer consumer;

    @Test
    void processaMensagemEPublicaResultado() throws Exception {
        JsonObject carga = new JsonObject().put("documentId", 10);
        OcrRequest requisicao = new OcrRequest(10L, "por", "aA==");
        OcrResponse resposta = new OcrResponse(10L, List.of("texto"), 90);
        when(objectMapper.readValue(carga.encode(), OcrRequest.class)).thenReturn(requisicao);
        when(ocrService.processar(requisicao)).thenReturn(resposta);

        consumer.processar(carga);

        verify(resultPublisher).publicar(resposta);
    }

    @Test
    void rejeitaMensagemJsonInvalidaRecebidaPelaFila() throws Exception {
        JsonObject carga = new JsonObject().put("documentId", 10);
        when(objectMapper.readValue(carga.encode(), OcrRequest.class))
                .thenThrow(new JsonProcessingException("JSON inválido") { });

        assertThrows(IllegalArgumentException.class, () -> consumer.processar(carga));
        verify(ocrService, never()).processar(org.mockito.ArgumentMatchers.any());
        verify(resultPublisher, never()).publicar(org.mockito.ArgumentMatchers.any());
    }
}
