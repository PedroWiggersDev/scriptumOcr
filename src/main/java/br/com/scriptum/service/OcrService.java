package br.com.scriptum.service;

import br.com.scriptum.DTO.request.OcrRequest;
import br.com.scriptum.DTO.response.OcrResponse;
import br.com.scriptum.service.decodificaImagem.DecodificadorImagem;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.jboss.logging.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

@ApplicationScoped
public class OcrService {

    private static final Logger LOG = Logger.getLogger(OcrService.class);

    private final DecodificadorImagem decodificadorImagem;
    private Tesseract motorOcr;

    /**
     * Cria o serviço com o componente responsável por decodificar imagens.
     *
     * @param decodificadorImagem decodificador de imagens Base64
     */
    public OcrService(DecodificadorImagem decodificadorImagem) {
        this.decodificadorImagem = decodificadorImagem;
    }

    /**
     * Inicializa o Tesseract e copia os arquivos de idioma suportados.
     */
    @PostConstruct
    void inicializar() {
        LOG.info("Inicializando o motor OCR Tesseract");
        try {
            Path caminhoDadosTesseract = Files.createTempDirectory("scriptum-tessdata");
            copiarRecurso("tessdata/por.traineddata", caminhoDadosTesseract);
            copiarRecurso("tessdata/eng.traineddata", caminhoDadosTesseract);

            motorOcr = new Tesseract();
            motorOcr.setDatapath(caminhoDadosTesseract.toString());
            motorOcr.setLanguage("por+eng");
            LOG.info("Motor OCR Tesseract inicializado com os idiomas por e eng");
        } catch (IOException exception) {
            LOG.fatal("Falha fatal ao inicializar os dados do motor OCR Tesseract", exception);
            throw new IllegalStateException(
                    "não foi possível inicializar o OCR Tesseract", exception);
        }
    }

    /**
     * Decodifica uma imagem Base64 e executa o OCR no idioma solicitado.
     *
     * @param requisicao solicitação com os dados do documento e da imagem
     * @return resposta com os parágrafos e a confiança da leitura
     */
    public OcrResponse processar(OcrRequest requisicao) {
        LOG.debugf("Decodificando imagem do documento %d", requisicao.documentId());
        BufferedImage imagem = decodificadorImagem.decodificar(requisicao.imageBase64());
        return processarImagem(requisicao.documentId(), requisicao.language(), imagem);
    }

    /**
     * Executa o OCR em uma imagem já decodificada.
     *
     * @param idDocumento identificador do documento
     * @param idioma idioma do OCR
     * @param imagem imagem decodificada
     * @return resposta com os parágrafos e a confiança da leitura
     */
    public synchronized OcrResponse processarImagem(
            long idDocumento, String idioma, BufferedImage imagem) {
        try {
            motorOcr.setLanguage(idioma);
            String texto = motorOcr.doOCR(imagem);
            List<String> paragrafos = dividirParagrafos(texto);
            OptionalDouble confianca = motorOcr.getWords(imagem, 3).stream()
                    .mapToDouble(Word::getConfidence)
                    .average();
            OcrResponse resposta = new OcrResponse(idDocumento, paragrafos, confianca.orElse(0));
            LOG.infof("OCR concluído para o documento %d: %d parágrafo(s), confiança %.2f",
                    idDocumento, paragrafos.size(), resposta.confidence());
            return resposta;
        } catch (TesseractException exception) {
            LOG.errorf(exception, "Falha ao executar OCR no documento %d", idDocumento);
            throw new IllegalStateException(
                    "não foi possível processar a imagem com o Tesseract", exception);
        }
    }

    /**
     * Divide o texto do OCR em parágrafos não vazios e sem espaços excedentes.
     *
     * @param texto texto bruto retornado pelo OCR
     * @return parágrafos extraídos
     */
    public static List<String> dividirParagrafos(String texto) {
        return Arrays.stream(texto.split("\\R\\s*\\R"))
                .map(String::trim)
                .filter(paragrafo -> !paragrafo.isEmpty())
                .toList();
    }

    /**
     * Copia um recurso de idioma do Tesseract para o diretório de dados.
     *
     * @param nomeRecurso nome do recurso no classpath
     * @param destino diretório de destino
     * @throws IOException quando o recurso não pode ser lido ou copiado
     */
    private void copiarRecurso(String nomeRecurso, Path destino) throws IOException {
        String nomeArquivo = Path.of(nomeRecurso).getFileName().toString();
        Path destinoArquivo = destino.resolve(nomeArquivo);

        try (InputStream entrada = getClass().getClassLoader()
                .getResourceAsStream(nomeRecurso)) {
            if (entrada == null) {
                throw new IOException("recurso do Tesseract não encontrado: " + nomeRecurso);
            }
            Files.copy(entrada, destinoArquivo);
        }
    }
}
