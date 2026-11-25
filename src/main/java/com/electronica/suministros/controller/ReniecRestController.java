package com.electronica.suministros.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.electronica.suministros.dto.ReniecDTO;
import com.electronica.suministros.dto.RucDTO;
import com.electronica.suministros.service.ReniecService;

/**
 * Avance final (integraciones): consultas de ApiPeru.dev para el botón "Buscar RENIEC / SUNAT" del modal de clientes.
 *   GET /api/reniec/{dni}      -> nombres según RENIEC (DNI de 8 dígitos)
 *   GET /api/reniec/ruc/{ruc}  -> razón social según SUNAT (RUC de 11 dígitos)
 * Protegido por AuthInterceptor (EJECUTIVO y ADMIN). Errores: {"error": "mensaje"} con el código HTTP
 * correspondiente (400 documento inválido, 404 no encontrado, 503 token no configurado, 502 API caída).
 */
@RestController
@RequestMapping("/api/reniec")
public class ReniecRestController {

    private final ReniecService reniecService;

    public ReniecRestController(ReniecService reniecService) {
        this.reniecService = reniecService;
    }

    @GetMapping("/{dni}")
    public ReniecDTO consultar(@PathVariable String dni) {
        return reniecService.consultar(dni);
    }

    @GetMapping("/ruc/{ruc}")
    public RucDTO consultarRuc(@PathVariable String ruc) {
        return reniecService.consultarRuc(ruc);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException e) {
        String mensaje = e.getReason() == null ? "No se pudo consultar RENIEC." : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", mensaje));
    }
}
