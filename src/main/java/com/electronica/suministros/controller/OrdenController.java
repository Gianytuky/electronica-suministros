package com.electronica.suministros.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.electronica.suministros.dto.TipoCambioDTO;
import com.electronica.suministros.entity.ComprobantePago;
import com.electronica.suministros.entity.OrdenTrabajo;
import com.electronica.suministros.service.CategoriaService;
import com.electronica.suministros.service.ClienteService;
import com.electronica.suministros.service.CorreoException;
import com.electronica.suministros.service.EmailService;
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.OrdenService;
import com.electronica.suministros.service.PdfService;
import com.electronica.suministros.service.TipoCambioService;

/**
 * Órdenes de Trabajo para el EJECUTIVO (y el ADMIN): listado con filtros, registro, edición, detalle,
 * cancelación, cobro (SP_Liquidar_Orden) y boleta en PDF. El formulario de registro/edición envía su JSON
 * a OrdenesRestController (fetch) porque una OT tiene un número variable de líneas.
 * Avance final: la boleta de una OT finalizada también se puede enviar por correo al cliente (PDF adjunto).
 */
@Controller
@RequestMapping("/ejecutivo/ordenes")
public class OrdenController {

    public static final List<String> ESTADOS = List.of(
            OrdenTrabajo.EN_RECEPCION, OrdenTrabajo.POR_NOTIFICAR, OrdenTrabajo.FINALIZADO, OrdenTrabajo.CANCELADO);

    private static final Logger log = LoggerFactory.getLogger(OrdenController.class);

    private final OrdenService ordenService;
    private final ClienteService clienteService;
    private final CategoriaService categoriaService;
    private final PdfService pdfService;
    private final TipoCambioService tipoCambioService;
    private final EmailService emailService;

    public OrdenController(OrdenService ordenService, ClienteService clienteService,
                           CategoriaService categoriaService, PdfService pdfService,
                           TipoCambioService tipoCambioService, EmailService emailService) {
        this.ordenService = ordenService;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.pdfService = pdfService;
        this.tipoCambioService = tipoCambioService;
        this.emailService = emailService;
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
        Boleta boleta;
        try {
            boleta = generarBoleta(id);
        } catch (NegocioException e) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(boleta.archivo()).build());
        return ResponseEntity.ok().headers(headers).body(boleta.pdf());
    }

    /**
     * Avance final: envía la boleta en PDF al correo del cliente (botón «Enviar boleta por correo»).
     * El envío es síncrono para que el Ejecutivo sepa al instante si el correo salió o falló.
     * "volver" indica a qué pantalla regresar: el listado (por defecto) o el detalle de la OT.
     */
    @PostMapping("/{id}/enviar-boleta")
    public String enviarBoleta(@PathVariable Integer id,
                               @RequestParam(defaultValue = "") String volver,
                               RedirectAttributes flash) {
        String destino = "detalle".equals(volver) ? "redirect:/ejecutivo/ordenes/" + id : "redirect:/ejecutivo/ordenes";
        try {
            Boleta boleta = generarBoleta(id);
            ComprobanteDTO c = boleta.datos();
            String correo = c.getCorreoCliente();
            if (correo == null || correo.isBlank()) {
                flash.addFlashAttribute("error", "El cliente " + c.getCliente() + " no tiene correo registrado. "
                        + "Regístrelo en Clientes para enviarle la boleta " + c.getNumero() + ".");
                return destino;
            }
            emailService.boletaElectronica(correo, c.getCliente(), c.getNumero(), c.getCodigoOt(),
                    c.getTotal(), boleta.archivo(), boleta.pdf());
            flash.addFlashAttribute("ok", "Boleta " + c.getNumero() + " enviada por correo a "
                    + emailService.describirDestino(correo) + ".");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (CorreoException | IllegalStateException e) {
            log.warn("No se pudo enviar la boleta de la OT {} por correo: {}", id, e.getMessage());
            flash.addFlashAttribute("error", "No se pudo enviar la boleta por correo. "
                    + "Verifique la configuración del correo e intente nuevamente.");
        }
        return destino;
    }

    /** Datos de la boleta, su PDF y el nombre del archivo. */
    private record Boleta(ComprobanteDTO datos, byte[] pdf, String archivo) {
    }

    /** Arma la boleta y su PDF; lo usan la descarga y el envío por correo. Lanza NegocioException si la OT no tiene boleta. */
    private Boleta generarBoleta(Integer id) {
        ComprobanteDTO dto = ordenService.armarComprobante(id);
        // Avance final (integraciones): monto referencial en dólares (tipo de cambio en caché; nunca bloquea la boleta)
        TipoCambioDTO tc = tipoCambioService.actual();
        dto.setTipoCambio(tc.getVenta());
        dto.setMontoDolares(tc.aDolares(dto.getTotal()));
        dto.setTipoCambioNota(tc.isRespaldo()
                ? "valor de respaldo configurado; ApiPeru.dev no estuvo disponible, " + tc.getConsultadoEn()
                : "venta del " + tc.getFecha() + ", ApiPeru.dev, consultado el " + tc.getConsultadoEn());
        byte[] pdf = pdfService.generar("pdf/comprobante-pdf", Map.of("c", dto));
        return new Boleta(dto, pdf, "Boleta_" + dto.getNumero() + ".pdf");
    }
}
