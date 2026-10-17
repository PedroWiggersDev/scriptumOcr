package br.com.scriptum.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Representa uma solicitação de leitura de documento.
 *
 * @param documentId identificador do documento
 * @param language idioma do documento, limitado a por ou eng
 * @param imageBase64 imagem codificada em Base64
 */
public record OcrRequest(
        @NotNull Long documentId,
        @NotBlank
        @Pattern(regexp = "por|eng", message = "language deve ser por ou eng")
        String language,
        @NotBlank String imageBase64
) {
    public OcrRequest {
        if (documentId == null || documentId < 0) {
            throw new IllegalArgumentException("documentId deve ser um long não negativo");
        }
        if (language != null) {
            language = language.toLowerCase(java.util.Locale.ROOT);
        }
    }
}
