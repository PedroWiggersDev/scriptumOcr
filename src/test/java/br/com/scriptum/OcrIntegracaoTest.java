package br.com.scriptum;

import br.com.scriptum.configuracao.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Any;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.wildfly.common.Assert.assertTrue;

@QuarkusTest
@IntegrationTest
class OcrIntegracaoTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    ObjectMapper objectMapper;

    private InMemorySource<JsonObject> source;
    private InMemorySink<String> sink;

    @BeforeEach
    void limpaMensageria() {
        source = connector.source("ocr-processing");
        sink = connector.sink("ocr-reading-return");
        sink.clear();
    }

    @Test
    void processaMensagemDeEntradaEPublicaRespostaNaFilaDeRetorno() throws Exception {
        source.send(new JsonObject()
            .put("documentId", 7357)
                .put("language", "eng")
                .put("imageBase64", imagemBase64()));

        String payload = aguardaPayload();
        JsonNode resposta = objectMapper.readTree(payload);

        assertEquals(7357, resposta.get("documentId").asLong());
        assertFalse(resposta.get("paragraphs").isEmpty());
        assertTrue(resposta.get("confidence").asDouble() > 0);
    }

    @Test
    void processaSolicitacaoPeloEndpointRest() throws Exception {
        given()
                .contentType("application/json")
                .body(new JsonObject()
                        .put("documentId", 7357)
                        .put("language", "eng")
                        .put("imageBase64", imagemBase64())
                        .encode())
                .when()
                .post("/ocr")
                .then()
                .statusCode(200)
                .body("documentId", equalTo(7357))
                .body("paragraphs.size()", greaterThan(0))
                .body("confidence", greaterThan(0f));
    }

    private String aguardaPayload() {
        Instant limite = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(limite)) {
            if (!sink.received().isEmpty()) {
                return sink.received().getFirst().getPayload();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                fail("interrompido aguardando resposta da fila", exception);
            }
        }
        fail("nenhuma resposta recebida na fila ocr-reading-return");
        return "";
    }

    private static String imagemBase64() throws IOException {
        try (InputStream imagem = OcrIntegracaoTest.class
                .getResourceAsStream("/ocr-test.png")) {
            if (imagem == null) {
                throw new IOException("fixture de imagem OCR não encontrado");
            }
            return Base64.getEncoder().encodeToString(imagem.readAllBytes());
        }
    }
}
