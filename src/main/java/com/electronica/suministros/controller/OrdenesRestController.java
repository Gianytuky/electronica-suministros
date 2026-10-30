package com.electronica.suministros.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.electronica.suministros.config.AuthInterceptor;
import com.electronica.suministros.dto.ClienteResumenDTO;
import com.electronica.suministros.dto.LineaDetalleDTO;
import com.electronica.suministros.dto.LineaEdicionDTO;
import com.electronica.suministros.dto.OrdenGuardadaDTO;
import com.electronica.suministros.dto.OrdenRequest;
import com.electronica.suministros.dto.OrdenResumenDTO;
import com.electronica.suministros.dto.UsuarioSesionDTO;
import com.electronica.suministros.entity.OrdenTrabajo;
import com.electronica.suministros.service.ClienteService;
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.OrdenService;

import jakarta.servlet.http.HttpSession;

/**
 * API privada (EJECUTIVO y ADMIN) que alimenta el formulario de OT y la consulta en cascada
 * Cliente -> OT -> Detalle. Devuelve records/DTO que Jackson convierte a JSON.
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenesRestController {

    private final OrdenService ordenService;
    private final ClienteService clienteService;

    public OrdenesRestController(OrdenService ordenService, ClienteService clienteService) {
        this.ordenService = ordenService;
        this.clienteService = clienteService;
    }

    /** RF-04: registrar una OT nueva. */
    @PostMapping
    public OrdenGuardadaDTO registrar(@RequestBody OrdenRequest request, HttpSession session) {
        UsuarioSesionDTO actual = (UsuarioSesionDTO) session.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        OrdenTrabajo ot = ordenService.registrar(request, actual.getIdUsuario());
        return new OrdenGuardadaDTO(ot.getIdOt(), ot.getCodigoOt(), ot.getMontoEstimado(),
                "OT " + ot.getCodigoOt() + " registrada en estado En Recepción.");
    }

    /** RF-05: editar una OT (solo si sigue En Recepción y sin captura técnica). */
    @PutMapping("/{id}")
    public OrdenGuardadaDTO actualizar(@PathVariable Integer id, @RequestBody OrdenRequest request) {
        OrdenTrabajo ot = ordenService.actualizar(id, request);
        return new OrdenGuardadaDTO(ot.getIdOt(), ot.getCodigoOt(), ot.getMontoEstimado(),
                "OT " + ot.getCodigoOt() + " actualizada correctamente.");
    }

    @GetMapping("/{id}/lineas")
    public List<LineaEdicionDTO> lineas(@PathVariable Integer id) {
        return ordenService.lineasParaEdicion(id);
    }

    // ---- cascada de la consulta: Cliente -> OT -> Detalle ----

    @GetMapping("/clientes")
    public List<ClienteResumenDTO> clientes() {
        return clienteService.resumenConOrdenes();
    }

    @GetMapping("/cliente/{idCliente}")
    public List<OrdenResumenDTO> ordenesDeCliente(@PathVariable Integer idCliente) {
        return ordenService.ordenesDeCliente(idCliente);
    }

    @GetMapping("/{id}/detalle")
    public List<LineaDetalleDTO> detalle(@PathVariable Integer id) {
        return ordenService.detalleJson(id);
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, String>> negocio(NegocioException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
