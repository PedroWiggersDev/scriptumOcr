package br.com.scriptum.configuration;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EnvironmentRouteFilter {

    @ConfigProperty(name = "app.environment", defaultValue = "production")
    String environment;

    @RouteFilter(1)
    void filterProductionRoutes(RoutingContext context) {
        if (EnvironmentRoutePolicy.isAllowed(environment, context.normalizedPath())) {
            context.next();
            return;
        }

        context.response()
                .setStatusCode(404)
                .end();
    }
}
