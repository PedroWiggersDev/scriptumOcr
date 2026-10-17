package br.com.scriptum.resource;

import br.com.scriptum.DTO.request.OcrRequest;
import br.com.scriptum.DTO.response.OcrResponse;
import br.com.scriptum.messaging.OcrResultPublisher;
import br.com.scriptum.service.OcrService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/ocr")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OcrResource {

    private static final Logger LOG = Logger.getLogger(OcrResource.class);

    @Inject
    OcrService ocrService;

    @Inject
    OcrResultPublisher resultPublisher;

    /**
     * Processa uma solicitação de OCR de forma síncrona e publica sua resposta.
     *
     * @param requisicao solicitação com identificador, idioma e imagem do documento
     * @return resposta do OCR ou HTTP 400 quando a imagem for inválida
     */
    @POST
    public Response processar(@Valid OcrRequest requisicao) {
        LOG.infof("Iniciando processamento OCR do documento %d no idioma %s",
                requisicao.documentId(), requisicao.language());
        final OcrResponse resposta;
        try {
            resposta = ocrService.processar(requisicao);
        } catch (IllegalArgumentException exception) {
            LOG.warnf("Solicitação OCR inválida para o documento %d: %s",
                    requisicao.documentId(), exception.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(exception.getMessage())
                    .build();
        }

        resultPublisher.publicar(resposta);
        LOG.infof("Processamento OCR concluído para o documento %d: %d parágrafo(s), confiança %.2f",
                resposta.documentId(), resposta.paragraphs().size(), resposta.confidence());
        return Response.ok(resposta).build();
    }
}
