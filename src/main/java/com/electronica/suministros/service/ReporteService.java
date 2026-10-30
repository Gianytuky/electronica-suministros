package com.electronica.suministros.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.dto.ConteoDTO;
import com.electronica.suministros.dto.FormatoIngresoDTO;
import com.electronica.suministros.dto.ReporteVentasDTO;
import com.electronica.suministros.entity.OrdenTrabajo;
import com.electronica.suministros.repository.CapturaTecnicaRepository;
import com.electronica.suministros.repository.ClienteRepository;
import com.electronica.suministros.repository.ComprobantePagoRepository;
import com.electronica.suministros.repository.DetalleOrdenRepository;
import com.electronica.suministros.repository.MensajeContactoRepository;
import com.electronica.suministros.repository.OrdenTrabajoRepository;
import com.electronica.suministros.repository.ReporteVentasRepository;

/** Indicadores gerenciales del Dashboard y de la pantalla de Reportes (RF-10). */
@Service
public class ReporteService {

    private final ReporteVentasRepository reporteRepo;
    private final OrdenTrabajoRepository ordenRepo;
    private final ComprobantePagoRepository comprobanteRepo;
    private final CapturaTecnicaRepository capturaRepo;
    private final DetalleOrdenRepository detalleRepo;
    private final ClienteRepository clienteRepo;
    private final MensajeContactoRepository mensajeRepo;

    public ReporteService(ReporteVentasRepository reporteRepo, OrdenTrabajoRepository ordenRepo,
                          ComprobantePagoRepository comprobanteRepo, CapturaTecnicaRepository capturaRepo,
                          DetalleOrdenRepository detalleRepo, ClienteRepository clienteRepo,
                          MensajeContactoRepository mensajeRepo) {
        this.reporteRepo = reporteRepo;
        this.ordenRepo = ordenRepo;
        this.comprobanteRepo = comprobanteRepo;
        this.capturaRepo = capturaRepo;
        this.detalleRepo = detalleRepo;
        this.clienteRepo = clienteRepo;
        this.mensajeRepo = mensajeRepo;
    }

    /** Años disponibles en el selector: solo los que tienen registros de ingresos. */
    public List<Integer> aniosDisponibles() {
        return List.of(2026, 2025);
    }

    /** Llama a SP_Reporte_Ventas y completa con ceros los meses sin boletas, para tener siempre 12 filas. */
    @Transactional(readOnly = true)
    public List<ReporteVentasDTO> ventasPorMes(int anio) {
        Map<Integer, ReporteVentasDTO> porMes = new LinkedHashMap<>();
        for (ReporteVentasDTO r : reporteRepo.ventasPorMes(anio)) {
            porMes.put(r.getMes(), r);
        }
        List<ReporteVentasDTO> completo = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            completo.add(porMes.getOrDefault(mes, new ReporteVentasDTO(mes, 0L, BigDecimal.ZERO)));
        }
        return completo;
    }

    /** Tarjetas (KPI) del dashboard. */
    @Transactional(readOnly = true)
    public Map<String, Object> kpis() {
        Map<String, Object> k = new LinkedHashMap<>();
        long enRecepcion = ordenRepo.countByEstado(OrdenTrabajo.EN_RECEPCION);
        long porNotificar = ordenRepo.countByEstado(OrdenTrabajo.POR_NOTIFICAR);
        long finalizadas = ordenRepo.countByEstado(OrdenTrabajo.FINALIZADO);
        long canceladas = ordenRepo.countByEstado(OrdenTrabajo.CANCELADO);
        k.put("ingresos", nz(comprobanteRepo.sumarIngresos()));
        k.put("porCobrar", nz(ordenRepo.sumarMontoPorEstado(OrdenTrabajo.POR_NOTIFICAR)));
        k.put("totalOrdenes", enRecepcion + porNotificar + finalizadas + canceladas);
        k.put("enRecepcion", enRecepcion);
        k.put("porNotificar", porNotificar);
        k.put("finalizadas", finalizadas);
        k.put("canceladas", canceladas);
        k.put("horas", nz(capturaRepo.sumarHoras()));
        k.put("gb", nz(capturaRepo.sumarGb()));
        k.put("clientes", clienteRepo.count());
        k.put("mensajesNuevos", mensajeRepo.countByEstado("NUEVO"));
        return k;
    }

    /** SUM() devuelve null cuando no hay filas: se convierte en cero. */
    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    @Transactional(readOnly = true)
    public List<ConteoDTO> ordenesPorEstado() {
        return ordenRepo.contarPorEstado();
    }

    @Transactional(readOnly = true)
    public List<ConteoDTO> lineasPorCategoria() {
        return detalleRepo.contarLineasPorCategoria();
    }

    /** Proyección por interfaz del repositorio (para tablas HTML), sin filtro de año. */
    @Transactional(readOnly = true)
    public List<FormatoIngresoDTO> ingresosPorFormato() {
        return detalleRepo.ingresosPorFormato();
    }

    /** Proyección por interfaz del repositorio (para tablas HTML), filtrada por año. */
    @Transactional(readOnly = true)
    public List<FormatoIngresoDTO> ingresosPorFormato(int anio) {
        return detalleRepo.ingresosPorFormato(anio);
    }

    /** Mismo reporte pero como mapas simples, listos para serializar a JSON (los proxies de proyección no se serializan bien). Sin filtro de año. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ingresosPorFormatoJson() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (FormatoIngresoDTO f : detalleRepo.ingresosPorFormato()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("formato", f.getFormato());
            fila.put("categoria", f.getCategoria());
            fila.put("ordenes", f.getOrdenes());
            fila.put("unidades", f.getUnidades());
            fila.put("ingresos", f.getIngresos());
            lista.add(fila);
        }
        return lista;
    }

    /** Mismo reporte filtrado por año de cobro, listo para serializar a JSON. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ingresosPorFormatoJson(int anio) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (FormatoIngresoDTO f : detalleRepo.ingresosPorFormato(anio)) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("formato", f.getFormato());
            fila.put("categoria", f.getCategoria());
            fila.put("ordenes", f.getOrdenes());
            fila.put("unidades", f.getUnidades());
            fila.put("ingresos", f.getIngresos());
            lista.add(fila);
        }
        return lista;
    }
}
