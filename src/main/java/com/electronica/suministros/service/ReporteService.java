package com.electronica.suministros.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.electronica.suministros.config.ClienteHttp;
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

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    private final ReporteVentasRepository reporteRepo;
    private final OrdenTrabajoRepository ordenRepo;
    private final ComprobantePagoRepository comprobanteRepo;
    private final CapturaTecnicaRepository capturaRepo;
    private final DetalleOrdenRepository detalleRepo;
    private final ClienteRepository clienteRepo;
    private final MensajeContactoRepository mensajeRepo;
    // Avance final (integraciones): microservicio Python de análisis (timeout corto: 1 s conexión, 4 s lectura)
    private final RestClient pythonApi = ClienteHttp.crear(Duration.ofSeconds(1), Duration.ofSeconds(4));
    private final String pythonApiUrl;

    public ReporteService(ReporteVentasRepository reporteRepo, OrdenTrabajoRepository ordenRepo,
                          ComprobantePagoRepository comprobanteRepo, CapturaTecnicaRepository capturaRepo,
                          DetalleOrdenRepository detalleRepo, ClienteRepository clienteRepo,
                          MensajeContactoRepository mensajeRepo,
                          @Value("${app.python-api.url:http://localhost:8000}") String pythonApiUrl) {
        this.pythonApiUrl = pythonApiUrl.endsWith("/") ? pythonApiUrl.substring(0, pythonApiUrl.length() - 1) : pythonApiUrl;
        this.reporteRepo = reporteRepo;
        this.ordenRepo = ordenRepo;
        this.comprobanteRepo = comprobanteRepo;
        this.capturaRepo = capturaRepo;
        this.detalleRepo = detalleRepo;
        this.clienteRepo = clienteRepo;
        this.mensajeRepo = mensajeRepo;
    }

    /**
     * Años disponibles en el selector: los que tienen boletas emitidas (consulta a comprobante_pago),
     * de mayor a menor. Siempre incluye el año actual, aunque todavía no tenga comprobantes.
     */
    @Transactional(readOnly = true)
    public List<Integer> aniosDisponibles() {
        List<Integer> anios = new ArrayList<>();
        for (Number n : comprobanteRepo.aniosConIngresos()) {
            if (n != null && !anios.contains(n.intValue())) {
                anios.add(n.intValue());
            }
        }
        int actual = LocalDate.now().getYear();
        if (!anios.contains(actual)) {
            anios.add(actual);
        }
        anios.sort(Comparator.reverseOrder());
        return anios;
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

    /**
     * Avance final (integraciones): análisis estadístico del año en el microservicio Python (FastAPI).
     * Spring arma el historial (12 meses de SP_Reporte_Ventas e ingresos por formato) y lo envía a
     * POST {PYTHON_API_URL}/api/analisis. Tolerancia a fallos: si el microservicio no está levantado o tarda
     * más de 4 s, se devuelve {"disponible": false, "mensaje": ...} y el resto de Reportes sigue funcionando.
     * No es @Transactional: no se mantiene una conexión a la base de datos abierta mientras se espera a Python.
     */
    public Map<String, Object> analisis(int anio) {
        LocalDate hoy = LocalDate.now();
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("anio", anio);
        cuerpo.put("mesActual", hoy.getYear() == anio ? hoy.getMonthValue() : 12);
        List<Map<String, Object>> meses = new ArrayList<>();
        for (ReporteVentasDTO r : ventasPorMes(anio)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mes", r.getMes());
            m.put("cantidadOrdenes", r.getCantidadOrdenes());
            m.put("totalIngresos", r.getTotalIngresos());
            meses.add(m);
        }
        cuerpo.put("ventasMensuales", meses);
        cuerpo.put("formatos", ingresosPorFormatoJson(anio));
        try {
            Map<String, Object> resultado = pythonApi.post()
                    .uri(pythonApiUrl + "/api/analisis")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            if (resultado == null) {
                throw new IllegalStateException("respuesta vacía");
            }
            Map<String, Object> salida = new LinkedHashMap<>(resultado);
            salida.put("disponible", true);
            return salida;
        } catch (RuntimeException e) {
            log.warn("Análisis predictivo no disponible (microservicio Python en {}): {}", pythonApiUrl, e.getMessage());
            Map<String, Object> sinServicio = new LinkedHashMap<>();
            sinServicio.put("disponible", false);
            sinServicio.put("mensaje", "El análisis predictivo no está disponible en este momento "
                    + "(el microservicio Python no está levantado o no respondió).");
            return sinServicio;
        }
    }
}
