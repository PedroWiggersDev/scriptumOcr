package br.com.scriptum.configuration;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public class RabbitMqUrlConfigSource implements ConfigSource {

    private static final String URL_VARIABLE = "RABBITMQ_URL";
    private final Map<String, String> properties;

    public RabbitMqUrlConfigSource() {
        String url = System.getenv(URL_VARIABLE);
        properties = url != null && url.contains("://")
                ? parseUrl(url)
                : Map.of();
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return RabbitMqUrlConfigSource.class.getName();
    }

    @Override
    public int getOrdinal() {
        return 350;
    }

    static Map<String, String> parseUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("RABBITMQ_URL não contém uma URI válida", exception);
        }

        String scheme = uri.getScheme();
        boolean ssl;
        if ("amqp".equalsIgnoreCase(scheme)) {
            ssl = false;
        } else if ("amqps".equalsIgnoreCase(scheme)) {
            ssl = true;
        } else {
            throw new IllegalArgumentException("RABBITMQ_URL deve usar o esquema amqp:// ou amqps://");
        }

        String host = uri.getHost();
        String rawUserInfo = uri.getRawUserInfo();
        if (host == null || rawUserInfo == null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "RABBITMQ_URL deve incluir host e credenciais, sem query ou fragmento");
        }

        int separator = rawUserInfo.indexOf(':');
        if (separator <= 0) {
            throw new IllegalArgumentException("RABBITMQ_URL deve incluir usuário e senha");
        }

        int port = uri.getPort();
        if (port == -1) {
            port = ssl ? 5671 : 5672;
        } else if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("RABBITMQ_URL contém uma porta inválida");
        }

        String rawPath = uri.getRawPath();
        String virtualHost = rawPath == null || rawPath.length() <= 1
                ? "/"
                : decode(rawPath.substring(1));

        return Map.of(
                "rabbitmq-addresses", host + ":" + port,
                "rabbitmq-username", decode(rawUserInfo.substring(0, separator)),
                "rabbitmq-password", decode(rawUserInfo.substring(separator + 1)),
                "rabbitmq-virtual-host", virtualHost,
                "rabbitmq-ssl-enabled", Boolean.toString(ssl));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
