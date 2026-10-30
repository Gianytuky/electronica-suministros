package com.electronica.suministros.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.electronica.suministros.dto.ComprobanteDTO;
import com.electronica.suministros.entity.ComprobantePago;
import com.electronica.suministros.entity.OrdenTrabajo;
import com.electronica.suministros.service.CategoriaService;
import com.electronica.suministros.service.ClienteService;
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.OrdenService;
import com.electronica.suministros.service.PdfService;

/**
 * Órdenes de Trabajo para el EJECUTIVO (y el ADMIN): listado con filtros, registro, edición, detalle,
 * cancelación, cobro (SP_Liquidar_Orden) y boleta en PDF. El formulario de registro/edición envía su JSON
 * a OrdenesRestController (fetch) porque una OT tiene un número variable de líneas.
 */
@Controller
@RequestMapping("/ejecutivo/ordenes")
public class OrdenController {

    public static final List<String> ESTADOS = List.of(
            OrdenTrabajo.EN_RECEPCION, OrdenTrabajo.POR_NOTIFICAR, OrdenTrabajo.FINALIZADO, OrdenTrabajo.CANCELADO);

    private final OrdenService ordenService;
    private final ClienteService clienteService;
    private final CategoriaService categoriaService;
    private final PdfService pdfService;

    public OrdenController(OrdenService ordenService, ClienteService clienteService,
                           CategoriaService categoriaService, PdfService pdfService) {
        this.ordenService = ordenService;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String estado,
                         @RequestParam(defaultValue = "") String texto,
                         Model model) {
        model.addAttribute("ordenes", ordenService.listar(estado, texto));
        model.addAttribute("estados", ESTADOS);
        model.addAttribute("estado", estado);
        model.addAttribute("texto", texto);
        return "intranet/ejecutivo/ordenes";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("orden", null);
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("categorias", categoriaService.listarActivas());
        return "intranet/ejecutivo/orden_form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes flash) {
        OrdenTrabajo ot = ordenService.buscar(id);
        if (!ot.isEditable()) {
            flash.addFlashAttribute("error", "La OT " + ot.getCodigoOt()
                    + " ya no se puede editar: solo se edita en estado En Recepción y sin captura técnica.");
            return "redirect:/ejecutivo/ordenes";
        }
        model.addAttribute("orden", ot);
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("categorias", categoriaService.listarActivas());
        return "intranet/ejecutivo/orden_form";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, Model model) {
        model.addAttribute("orden", ordenService.buscar(id));
        model.addAttribute("lineas", ordenService.detalle(id));
        return "intranet/ejecutivo/orden_detalle";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            ordenService.cancelar(id);
            flash.addFlashAttribute("ok", "La orden de trabajo fue cancelada.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ejecutivo/ordenes";
    }

    /** Cobro y entrega: emite la boleta con SP_Liquidar_Orden y deja la OT en Finalizado. */
    @PostMapping("/{id}/liquidar")
    public String liquidar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            ComprobantePago cp = ordenService.liquidar(id);
            flash.addFlashAttribute("ok", "OT cobrada y entregada. Boleta " + cp.getNumeroComprobante()
                    + " emitida por S/ " + cp.getMontoFinal() + ".");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ejecutivo/ordenes";
    }

    @GetMapping("/{id}/comprobante")
    public ResponseEntity<byte[]> comprobante(@PathVariable Integer id) {
        ComprobanteDTO dto;
        try {
            dto = ordenService.armarComprobante(id);
        } catch (NegocioException e) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = pdfService.generar("pdf/comprobante-pdf", Map.of("c", dto));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("Boleta_" + dto.getNumero() + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
