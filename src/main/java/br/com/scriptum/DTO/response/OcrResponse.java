package br.com.scriptum.DTO.response;

import java.util.List;

/**
 * Representa o resultado da leitura de um documento.
 *
 * @param documentId identificador do documento
 * @param paragraphs parágrafos reconhecidos
 * @param confidence confiança média do reconhecimento
 */
public record OcrResponse(
        long documentId,
        List<String> paragraphs,
        double confidence
) {
    public OcrResponse {
        paragraphs = paragraphs == null ? List.of() : List.copyOf(paragraphs);
        if (Double.isNaN(confidence) || confidence < 0 || confidence > 100) {
            throw new IllegalArgumentException("confidence deve estar entre 0 e 100");
        }
    }
}
