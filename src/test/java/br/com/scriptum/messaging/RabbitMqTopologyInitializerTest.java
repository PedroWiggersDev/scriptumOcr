package br.com.scriptum.messaging;

import br.com.scriptum.configuracao.UnitTest;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RabbitMqTopologyInitializerTest {

    @Mock
    Channel channel;

    @Test
    void declaraExchangesFilasComDeadLetterEBindings() throws Exception {
        RabbitMqTopologyInitializer.declareTopology(channel);

        verify(channel).exchangeDeclare("ocr.processing", "direct", true, false, Map.of());
        verify(channel).exchangeDeclare("ocr.reading", "direct", true, false, Map.of());
        verify(channel).exchangeDeclare("ocr.processing.dlx", "direct", true, false, Map.of());
        verify(channel).queueDeclare("ocr-processing", true, false, false, Map.of(
                "x-dead-letter-exchange", "ocr.processing.dlx",
                "x-dead-letter-routing-key", "ocr.processing.failed"));
        verify(channel).queueDeclare("ocr-processing-dlq", true, false, false, Map.of());
        verify(channel).queueDeclare("ocr-reading-return", true, false, false, Map.of());
        verify(channel).queueBind("ocr-processing", "ocr.processing", "ocr.processing");
        verify(channel).queueBind("ocr-reading-return", "ocr.reading", "ocr.reading");
        verify(channel).queueBind("ocr-processing-dlq", "ocr.processing.dlx", "ocr.processing.failed");
    }
}
