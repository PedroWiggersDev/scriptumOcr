package br.com.scriptum;

import br.com.scriptum.DTO.response.OcrResponse;
import br.com.scriptum.configuracao.UnitTest;
import br.com.scriptum.messaging.OcrResultPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.reactive.messaging.Emitter;
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
class OcrResultPublisherTest {

    @Mock
    private Emitter<String> returnEmitter;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OcrResultPublisher publisher;

    @Test
    void falhaExplicitamenteQuandoNaoConsegueSerializarResultado() throws Exception {
        OcrResponse resposta = new OcrResponse(10L, List.of("texto"), 90);
        when(objectMapper.writeValueAsString(resposta))
                .thenThrow(new JsonProcessingException("falha de serialização") { });

        assertThrows(IllegalStateException.class, () -> publisher.publicar(resposta));
        verify(returnEmitter, never()).send(org.mockito.ArgumentMatchers.anyString());
    }
}
