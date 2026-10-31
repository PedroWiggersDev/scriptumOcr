package br.com.scriptum.configuracao;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class ProductionEndpointProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("app.environment", "production");
    }
}
