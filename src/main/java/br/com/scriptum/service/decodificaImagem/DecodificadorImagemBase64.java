package br.com.scriptum.service.decodificaImagem;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Decodifica imagens Base64 usando as extensões de imagem disponíveis na JVM.
 */
@ApplicationScoped
public class DecodificadorImagemBase64 implements DecodificadorImagem {

    private static final Logger LOG = Logger.getLogger(DecodificadorImagemBase64.class);

    /**
     * Converte o conteúdo Base64 em uma imagem.
     *
     * @param imagemBase64 imagem codificada em Base64
     * @return imagem decodificada
     */
    @Override
    public BufferedImage decodificar(String imagemBase64) {
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(imagemBase64);
        } catch (IllegalArgumentException exception) {
            LOG.warn("Imagem recebida com Base64 inválido");
            throw new IllegalArgumentException("imagemBase64 não é um Base64 válido", exception);
        }

        try (ByteArrayInputStream entrada = new ByteArrayInputStream(bytes)) {
            BufferedImage imagem = ImageIO.read(entrada);
            if (imagem == null) {
                LOG.warn("Imagem recebida não possui um formato reconhecido");
                throw new IllegalArgumentException("imagemBase64 não contém uma imagem válida");
            }
            return imagem;
        } catch (IOException exception) {
            LOG.error("Falha ao ler a imagem recebida", exception);
            throw new IllegalArgumentException("não foi possível ler a imagem", exception);
        }
    }
}
