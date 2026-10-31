package br.com.scriptum;

import br.com.scriptum.configuracao.IntegrationTest;
import br.com.scriptum.configuracao.ProductionEndpointProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
@IntegrationTest
@TestProfile(ProductionEndpointProfile.class)
class ProductionEndpointPolicyTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    ObjectMapper objectMapper;

    private InMemorySource<JsonObject> source;
    private InMemorySink<String> sink;

    @BeforeEach
    void preparaMensageria() {
        source = connector.source("ocr-processing");
        sink = connector.sink("ocr-reading-return");
        sink.clear();
    }

    @Test
    void mantemEndpointDeVersaoDisponivel() {
        given()
                .when()
                .get("/version")
                .then()
                .statusCode(200)
                .body("version", not(emptyOrNullString()));
    }

    @Test
    void bloqueiaEndpointOcrEmProducao() {
        given()
                .when()
                .post("/ocr")
                .then()
                .statusCode(404);
    }

    @Test
    void bloqueiaSwaggerUiEOpenApiEmProducao() {
        given()
                .when()
                .get("/q/swagger-ui/")
                .then()
                .statusCode(404);

        given()
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(404);
    }

    @Test
    void mantemEndpointsOperacionaisDisponiveisEmProducao() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200);
    }

    @Test
    void mantemConsumoEPublicacaoRabbitMqAtivosEmProducao() throws Exception {
        source.send(new JsonObject()
                .put("documentId", 7358)
                .put("language", "eng")
                .put("imageBase64", imagemBase64()));

        JsonNode resposta = objectMapper.readTree(aguardaPayload());
        assertEquals(7358, resposta.get("documentId").asLong());
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
        try (InputStream imagem = ProductionEndpointPolicyTest.class
                .getResourceAsStream("/ocr-test.png")) {
            if (imagem == null) {
                throw new IOException("fixture de imagem OCR não encontrado");
            }
            return Base64.getEncoder().encodeToString(imagem.readAllBytes());
        }
    }
}
