package com.electronica.suministros.service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Convierte una plantilla Thymeleaf (XHTML bien formado) en un PDF con Flying Saucer + OpenPDF.
 * Se usa para la boleta de la OT cobrada.
 */
@Service
public class PdfService {

    private final TemplateEngine templateEngine;

    public PdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generar(String plantilla, Map<String, Object> variables) {
        Context contexto = new Context();
        contexto.setVariables(variables);
        String html = templateEngine.process(plantilla, contexto);

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(salida);
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }
}
