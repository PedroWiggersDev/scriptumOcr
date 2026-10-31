package br.com.scriptum;

import br.com.scriptum.configuration.EnvironmentRoutePolicy;
import br.com.scriptum.configuracao.UnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UnitTest
class EnvironmentRoutePolicyTest {

    @Test
    void permiteTodasAsRotasEmHomologacaoSemDiferenciarMaiusculas() {
        assertTrue(EnvironmentRoutePolicy.isAllowed("staging", "/ocr"));
        assertTrue(EnvironmentRoutePolicy.isAllowed("STAGING", "/q/swagger-ui/"));
    }

    @Test
    void assumeProducaoQuandoAmbienteNaoEstaConfigurado() {
        assertTrue(EnvironmentRoutePolicy.isAllowed(null, "/version"));
        assertFalse(EnvironmentRoutePolicy.isAllowed(null, "/ocr"));
    }

    @Test
    void permiteSomenteVersaoEEndpointsOperacionaisEmProducao() {
        assertTrue(EnvironmentRoutePolicy.isAllowed("production", "/version"));
        assertTrue(EnvironmentRoutePolicy.isAllowed("production", "/q/health"));
        assertTrue(EnvironmentRoutePolicy.isAllowed("production", "/q/health/ready"));
        assertTrue(EnvironmentRoutePolicy.isAllowed("production", "/q/metrics"));
        assertFalse(EnvironmentRoutePolicy.isAllowed("production", "/ocr"));
        assertFalse(EnvironmentRoutePolicy.isAllowed("production", "/q/swagger-ui/"));
        assertFalse(EnvironmentRoutePolicy.isAllowed("production", "/q/openapi"));
    }

    @Test
    void trataAmbienteDesconhecidoComoProducao() {
        assertTrue(EnvironmentRoutePolicy.isAllowed("unknown", "/version"));
        assertFalse(EnvironmentRoutePolicy.isAllowed("unknown", "/ocr"));
    }
}
