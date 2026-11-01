package br.com.scriptum.service;

import br.com.scriptum.DTO.request.OcrRequest;
import br.com.scriptum.DTO.response.OcrResponse;
import br.com.scriptum.configuracao.UnitTest;
import br.com.scriptum.service.decodificaImagem.DecodificadorImagem;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @Mock
    DecodificadorImagem decodificadorImagem;

    @Mock
    Tesseract motorOcr;

    @Mock
    BufferedImage imagem;

    @Test
    void processaRequisicaoECalculaConfiancaMedia() throws Exception {
        OcrService service = new OcrService(decodificadorImagem, motorOcr);
        OcrRequest request = new OcrRequest(21L, "eng", "base64-image");
        Word word1 = new Word("OCR", 80, null);
        Word word2 = new Word("TEST", 100, null);
        when(decodificadorImagem.decodificar("base64-image")).thenReturn(imagem);
        when(motorOcr.doOCR(imagem)).thenReturn("  primeiro parágrafo  \n\n segundo parágrafo \n\n");
        when(motorOcr.getWords(imagem, 3)).thenReturn(List.of(word1, word2));

        OcrResponse response = service.processar(request);

        assertEquals(21L, response.documentId());
        assertEquals(List.of("primeiro parágrafo", "segundo parágrafo"), response.paragraphs());
        assertEquals(90, response.confidence());
        verify(motorOcr).setLanguage("eng");
    }

    @Test
    void retornaConfiancaZeroQuandoNenhumaPalavraForReconhecida() throws Exception {
        OcrService service = new OcrService(decodificadorImagem, motorOcr);
        when(motorOcr.doOCR(imagem)).thenReturn(" \n\n ");
        when(motorOcr.getWords(imagem, 3)).thenReturn(List.of());

        OcrResponse response = service.processarImagem(22L, "por", imagem);

        assertEquals(List.of(), response.paragraphs());
        assertEquals(0, response.confidence());
    }

    @Test
    void propagaFalhaDeOcrComoIllegalStateException() throws Exception {
        OcrService service = new OcrService(decodificadorImagem, motorOcr);
        TesseractException failure = new TesseractException("OCR failure");
        when(motorOcr.doOCR(imagem)).thenThrow(failure);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.processarImagem(23L, "eng", imagem));

        assertEquals("não foi possível processar a imagem com o Tesseract", exception.getMessage());
        assertEquals(failure, exception.getCause());
    }

    @Test
    void divideTextoEmParagrafosIgnorandoEspacosVazios() {
        assertEquals(List.of("linha um \nlinha dois", "linha três"),
                OcrService.dividirParagrafos(" \n\n linha um \nlinha dois \n\n\n linha três \n\n "));
        assertEquals(List.of(), OcrService.dividirParagrafos(""));
    }

    @Test
    void copiaDadosDeIdiomaParaDiretorioDeDados(@TempDir Path tempDir) throws IOException {
        OcrService.copiarRecurso("tessdata/eng.traineddata", tempDir,
                OcrService.class.getClassLoader());

        assertEquals(Files.size(Path.of("src/main/resources/tessdata/eng.traineddata")),
                Files.size(tempDir.resolve("eng.traineddata")));
    }

    @Test
    void falhaExplicitamenteQuandoRecursoDeIdiomaNaoExiste(@TempDir Path tempDir) {
        IOException exception = assertThrows(IOException.class,
                () -> OcrService.copiarRecurso("tessdata/missing.traineddata", tempDir,
                        OcrService.class.getClassLoader()));

        assertEquals("recurso do Tesseract não encontrado: tessdata/missing.traineddata",
                exception.getMessage());
    }

    @Test
    void inicializaMotorComOsIdiomasSuportados() {
        OcrService service = new OcrService(decodificadorImagem);

        service.inicializar();
    }
}
