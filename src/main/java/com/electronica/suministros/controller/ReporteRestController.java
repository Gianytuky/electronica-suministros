package com.electronica.suministros.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.electronica.suministros.dto.ConteoDTO;
import com.electronica.suministros.dto.ReporteVentasDTO;
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.ReporteService;

/** Datos JSON para los gráficos Chart.js del dashboard y de reportes (solo ADMIN). */
@RestController
@RequestMapping("/api/reportes")
public class ReporteRestController {

    private final ReporteService reporteService;

    public ReporteRestController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Resultado de SP_Reporte_Ventas: 12 filas (una por mes) del año pedido. */
    @GetMapping("/ventas-mensuales")
    public List<ReporteVentasDTO> ventasMensuales(@RequestParam int anio) {
        if (anio < 2000 || anio > 2100) {
            throw new NegocioException("Año fuera de rango");
        }
        return reporteService.ventasPorMes(anio);
    }

    @GetMapping("/estados")
    public List<ConteoDTO> estados() {
        return reporteService.ordenesPorEstado();
    }

    @GetMapping("/categorias")
    public List<ConteoDTO> categorias() {
        return reporteService.lineasPorCategoria();
    }

    /** Ingresos por formato filtrados por a\u00f1o (OTs cobradas). Si no se pasa a\u00f1o, devuelve todos. */
    @GetMapping("/formatos")
    public List<Map<String, Object>> formatos(
            @RequestParam(required = false) Integer anio) {
        if (anio != null) {
            if (anio < 2000 || anio > 2100) {
                throw new NegocioException("A\u00f1o fuera de rango");
            }
            return reporteService.ingresosPorFormatoJson(anio);
        }
        return reporteService.ingresosPorFormatoJson();
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, String>> negocio(NegocioException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
