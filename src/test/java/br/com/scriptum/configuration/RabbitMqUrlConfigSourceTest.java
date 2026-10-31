package br.com.scriptum.configuration;

import br.com.scriptum.configuracao.UnitTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RabbitMqUrlConfigSourceTest {

    @UnitTest
    @Test
    void interpretaUriAmqpsComCredenciaisEVirtualHost() {
        Map<String, String> properties = RabbitMqUrlConfigSource.parseUrl(
                "amqps://my-user:p%40ss%3Aword@gerbil.rmq.cloudamqp.com/tenant");

        assertEquals("gerbil.rmq.cloudamqp.com:5671", properties.get("rabbitmq-addresses"));
        assertEquals("my-user", properties.get("rabbitmq-username"));
        assertEquals("p@ss:word", properties.get("rabbitmq-password"));
        assertEquals("tenant", properties.get("rabbitmq-virtual-host"));
        assertEquals("true", properties.get("rabbitmq-ssl-enabled"));
    }

    @UnitTest
    @Test
    void interpretaAmqpComPortaEVirtualHostPadrao() {
        Map<String, String> properties = RabbitMqUrlConfigSource.parseUrl(
                "amqp://scriptum:local-password@rabbitmq:5672/%2F");

        assertEquals("rabbitmq:5672", properties.get("rabbitmq-addresses"));
        assertEquals("/", properties.get("rabbitmq-virtual-host"));
        assertEquals("false", properties.get("rabbitmq-ssl-enabled"));
    }

    @UnitTest
    @Test
    void rejeitaUriSemEsquemaAmqpOuCredenciais() {
        assertThrows(IllegalArgumentException.class,
                () -> RabbitMqUrlConfigSource.parseUrl("https://user:password@rabbitmq"));
        assertThrows(IllegalArgumentException.class,
                () -> RabbitMqUrlConfigSource.parseUrl("amqps://rabbitmq/tenant"));
    }
}
