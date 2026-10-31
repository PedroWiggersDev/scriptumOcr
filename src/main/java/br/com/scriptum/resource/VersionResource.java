package br.com.scriptum.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {

    private final String version;

    @Inject
    public VersionResource(@ConfigProperty(name = "quarkus.application.version") String version) {
        this.version = version;
    }

    @GET
    public VersionResponse getVersion() {
        return new VersionResponse(version);
    }

    public record VersionResponse(String version) {
    }
}
