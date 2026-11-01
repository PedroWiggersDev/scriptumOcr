package br.com.scriptum.messaging;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class RabbitMqTopologyInitializer {

    private static final Logger LOG = Logger.getLogger(RabbitMqTopologyInitializer.class);

    @ConfigProperty(name = "rabbitmq-addresses")
    String addresses;

    @ConfigProperty(name = "rabbitmq-username")
    String username;

    @ConfigProperty(name = "rabbitmq-password")
    String password;

    @ConfigProperty(name = "rabbitmq-virtual-host")
    String virtualHost;

    @ConfigProperty(name = "rabbitmq-ssl-enabled")
    boolean sslEnabled;

    @ConfigProperty(name = "rabbitmq-topology.enabled", defaultValue = "true")
    boolean topologyEnabled;

    void initialize(@Observes @Priority(1) StartupEvent event) {
        if (!topologyEnabled) {
            LOG.info("Declaração da topologia RabbitMQ desabilitada pela configuração");
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setConnectionTimeout(10_000);

        try {
            if (sslEnabled) {
                factory.useSslProtocol();
            }
            declareTopology(factory);
        } catch (IOException | TimeoutException | java.security.NoSuchAlgorithmException
                 | java.security.KeyManagementException exception) {
            throw new IllegalStateException("Não foi possível inicializar a topologia do RabbitMQ", exception);
        }
    }

    private void declareTopology(ConnectionFactory factory) throws IOException, TimeoutException {
        Address[] brokerAddresses = Address.parseAddresses(addresses);
        try (Connection connection = factory.newConnection(brokerAddresses);
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare("ocr.processing", "direct", true, false, Map.of());
            channel.exchangeDeclare("ocr.reading", "direct", true, false, Map.of());
            channel.exchangeDeclare("ocr.processing.dlx", "direct", true, false, Map.of());

            channel.queueDeclare("ocr-processing", true, false, false, Map.of(
                    "x-dead-letter-exchange", "ocr.processing.dlx",
                    "x-dead-letter-routing-key", "ocr.processing.failed"));
            channel.queueDeclare("ocr-processing-dlq", true, false, false, Map.of());
            channel.queueDeclare("ocr-reading-return", true, false, false, Map.of());

            channel.queueBind("ocr-processing", "ocr.processing", "ocr.processing");
            channel.queueBind("ocr-reading-return", "ocr.reading", "ocr.reading");
            channel.queueBind("ocr-processing-dlq", "ocr.processing.dlx", "ocr.processing.failed");
        }

        LOG.infof("Topologia RabbitMQ declarada no virtual host '%s'", virtualHost);
    }
}
