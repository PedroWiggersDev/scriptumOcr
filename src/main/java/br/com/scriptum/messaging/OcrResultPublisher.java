package br.com.scriptum.messaging;

import br.com.scriptum.DTO.response.OcrResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OcrResultPublisher {

    private static final Logger LOG = Logger.getLogger(OcrResultPublisher.class);

    @Inject
    @Channel("ocr-reading-return")
    Emitter<String> returnEmitter;

    @Inject
    ObjectMapper objectMapper;

    public void publicar(OcrResponse resposta) {
        try {
            returnEmitter.send(objectMapper.writeValueAsString(resposta));
        } catch (JsonProcessingException exception) {
            LOG.error("Não foi possível serializar a resposta OCR", exception);
            throw new IllegalStateException(
                    "não foi possível serializar a mensagem de OCR", exception);
        }
    }
}
