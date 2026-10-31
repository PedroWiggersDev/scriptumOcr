package br.com.scriptum;

import br.com.scriptum.configuracao.UnitTest;
import br.com.scriptum.resource.VersionResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UnitTest
class VersionResourceTest {

    @Test
    void retornaVersaoConfiguradaNaResposta() {
        VersionResource resource = new VersionResource("1.2.3");

        assertEquals(new VersionResource.VersionResponse("1.2.3"), resource.getVersion());
    }
}
