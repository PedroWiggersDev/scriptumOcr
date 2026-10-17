package br.com.scriptum.service.decodificaImagem;

import java.awt.image.BufferedImage;

/**
 * Define a conversão de uma imagem codificada para uma imagem utilizável pelo OCR.
 */
public interface DecodificadorImagem {

    /**
     * Decodifica uma imagem.
     *
     * @param imagemBase64 imagem codificada em Base64
     * @return imagem decodificada
     */
    BufferedImage decodificar(String imagemBase64);
}
