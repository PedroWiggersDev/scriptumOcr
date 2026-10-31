package br.com.scriptum.configuration;

public final class EnvironmentRoutePolicy {

    private EnvironmentRoutePolicy() {
    }

    public static boolean isAllowed(String environment, String path) {
        if ("staging".equalsIgnoreCase(environment)) {
            return true;
        }

        return "/version".equals(path)
                || "/q/health".equals(path)
                || path.startsWith("/q/health/")
                || "/q/metrics".equals(path);
    }
}
