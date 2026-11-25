package com.electronica.suministros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.electronica.suministros.config.AuthInterceptor;
import com.electronica.suministros.dto.CapturaFormDTO;
import com.electronica.suministros.dto.UsuarioSesionDTO;
import com.electronica.suministros.entity.OrdenTrabajo;
import com.electronica.suministros.service.CapturaService;
import com.electronica.suministros.service.EmailService;
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.OrdenService;

import jakarta.servlet.http.HttpSession;

/** Panel del laboratorio (rol TECNICO y ADMIN): cola de OTs, captura técnica (RF-07 / RF-08) e historial. */
@Controller
@RequestMapping("/tecnico")
public class TecnicoController {

    private final CapturaService capturaService;
    private final OrdenService ordenService;
    private final EmailService emailService;

    public TecnicoController(CapturaService capturaService, OrdenService ordenService, EmailService emailService) {
        this.capturaService = capturaService;
        this.ordenService = ordenService;
        this.emailService = emailService;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        model.addAttribute("cola", capturaService.cola());
        model.addAttribute("totalHoras", capturaService.totalHoras());
        model.addAttribute("totalGb", capturaService.totalGb());
        model.addAttribute("capturas", capturaService.ultimas().stream().limit(5).toList());
        return "intranet/tecnico/panel";
    }

    @GetMapping("/ordenes/{id}")
    public String ordenParaCaptura(@PathVariable Integer id, Model model) {
        model.addAttribute("orden", ordenService.buscar(id));
        model.addAttribute("lineas", ordenService.detalle(id));
        model.addAttribute("medios", CapturaService.MEDIOS_ENTREGA);
        return "intranet/tecnico/captura";
    }

    @PostMapping("/captura/guardar")
    public String guardar(@Validated @ModelAttribute CapturaFormDTO form,
                          BindingResult resultado,
                          @RequestParam Integer idOt,
                          HttpSession session,
                          RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            flash.addFlashAttribute("error", resultado.getFieldError().getDefaultMessage());
            return "redirect:/tecnico/ordenes/" + idOt;
        }
        UsuarioSesionDTO actual = (UsuarioSesionDTO) session.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        try {
            OrdenTrabajo ot = capturaService.registrar(form, actual.getIdUsuario());
            if (OrdenTrabajo.POR_NOTIFICAR.equals(ot.getEstado())) {
                flash.addFlashAttribute("ok", "Captura guardada. Todas las líneas de la OT " + ot.getCodigoOt()
                        + " fueron procesadas: el total se recalculó a S/ " + ot.getMontoEstimado()
                        + " y la OT pasó a Por Notificar. " + textoAvisoCliente(ot));
            } else {
                flash.addFlashAttribute("ok", "Captura guardada. Aún quedan líneas de la OT " + ot.getCodigoOt()
                        + " por procesar; total parcial S/ " + ot.getMontoEstimado() + ".");
            }
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tecnico/ordenes/" + idOt;
    }

    /**
     * Frase informativa sobre el correo automático. El envío real ocurre después, en segundo plano
     * (NotificacionOrdenListener), por eso el texto anuncia el aviso en futuro y no lo da por enviado;
     * el resultado real queda en el log. Con el modo prueba activo indica a qué cuenta llegará de verdad.
     */
    private String textoAvisoCliente(OrdenTrabajo ot) {
        String correo = ot.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            return "El cliente no tiene correo registrado, por lo que no se enviará aviso.";
        }
        return "Se enviará un aviso por correo a " + emailService.describirDestino(correo) + ".";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        model.addAttribute("capturas", capturaService.ultimas());
        return "intranet/tecnico/historial";
    }
}
