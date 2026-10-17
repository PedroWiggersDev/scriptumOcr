package br.com.scriptum.messaging;

import br.com.scriptum.DTO.request.OcrRequest;
import br.com.scriptum.DTO.response.OcrResponse;
import br.com.scriptum.service.OcrService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OcrMessageConsumer {

    private static final Logger LOG = Logger.getLogger(OcrMessageConsumer.class);

    @Inject
    OcrService ocrService;

    @Inject
    OcrResultPublisher resultPublisher;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("ocr-processing")
    @Blocking
    public void processar(JsonObject carga) {
        final OcrRequest requisicao;
        try {
            requisicao = objectMapper.readValue(carga.encode(), OcrRequest.class);
        } catch (JsonProcessingException exception) {
            LOG.warn("Mensagem inválida recebida na fila ocr-processing", exception);
            throw new IllegalArgumentException("mensagem inválida na fila de OCR", exception);
        }

        OcrResponse resposta = ocrService.processar(requisicao);
        resultPublisher.publicar(resposta);
        LOG.infof("Mensagem da fila processada para o documento %d", resposta.documentId());
    }
}
