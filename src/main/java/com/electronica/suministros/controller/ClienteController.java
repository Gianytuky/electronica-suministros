package com.electronica.suministros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.electronica.suministros.service.ClienteService;
import com.electronica.suministros.service.NegocioException;

/** Mantenimiento de clientes (EJECUTIVO y ADMIN). Alta y edición se hacen en una ventana modal. */
@Controller
@RequestMapping("/ejecutivo/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String texto, Model model) {
        model.addAttribute("clientes", clienteService.buscar(texto));
        model.addAttribute("texto", texto);
        return "intranet/ejecutivo/clientes";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Integer idCliente,
                          @RequestParam String dniCliente,
                          @RequestParam String nombres,
                          @RequestParam(required = false) String telefono,
                          @RequestParam(required = false) String correo,
                          RedirectAttributes flash) {
        try {
            clienteService.guardar(idCliente, dniCliente, nombres, telefono, correo);
            flash.addFlashAttribute("ok", idCliente == null ? "Cliente registrado correctamente." : "Cliente actualizado correctamente.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ejecutivo/clientes";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            clienteService.eliminar(id);
            flash.addFlashAttribute("ok", "Cliente eliminado correctamente.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ejecutivo/clientes";
    }
}
