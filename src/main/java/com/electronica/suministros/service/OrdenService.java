package com.electronica.suministros.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.dto.ComprobanteDTO;
import com.electronica.suministros.dto.LineaDetalleDTO;
import com.electronica.suministros.dto.LineaEdicionDTO;
import com.electronica.suministros.dto.LineaRequest;
import com.electronica.suministros.dto.OrdenDetalleDTO;
import com.electronica.suministros.dto.OrdenListadoDTO;
import com.electronica.suministros.dto.OrdenRequest;
import com.electronica.suministros.dto.OrdenResumenDTO;
import com.electronica.suministros.entity.Cliente;
import com.electronica.suministros.entity.ComprobantePago;
import com.electronica.suministros.entity.DetalleOrden;
import com.electronica.suministros.entity.MantenimientoCinta;
import com.electronica.suministros.entity.OrdenTrabajo;
import com.electronica.suministros.entity.ServicioTarifario;
import com.electronica.suministros.entity.Usuario;
import com.electronica.suministros.repository.ClienteRepository;
import com.electronica.suministros.repository.ComprobantePagoRepository;
import com.electronica.suministros.repository.CorrelativoRepository;
import com.electronica.suministros.repository.DetalleOrdenRepository;
import com.electronica.suministros.repository.OrdenTrabajoRepository;
import com.electronica.suministros.repository.ServicioTarifarioRepository;
import com.electronica.suministros.repository.UsuarioRepository;

/**
 * Reglas de negocio de la Orden de Trabajo (OT):
 *   RF-04  registrar la OT con estado inicial "En Recepción" y monto estimado
 *   RF-05  editar solo mientras está "En Recepción" (y sin captura técnica)
 *   RF-06  bloquear la edición en "Por Notificar" y "Finalizado"
 *   cobro  SP_Liquidar_Orden emite la boleta y finaliza la OT
 */
@Service
public class OrdenService {

    /** Recargo fijo por limpieza / eliminación de hongos (RF-03). */
    public static final BigDecimal COSTO_LIMPIEZA = new BigDecimal("15.00");
    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");
    private static final int MAX_LINEAS = 20;
    private static final int MAX_CANTIDAD = 9999;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdenTrabajoRepository ordenRepo;
    private final ClienteRepository clienteRepo;
    private final UsuarioRepository usuarioRepo;
    private final ServicioTarifarioRepository servicioRepo;
    private final DetalleOrdenRepository detalleRepo;
    private final ComprobantePagoRepository comprobanteRepo;
    private final CorrelativoRepository correlativoRepo;

    public OrdenService(OrdenTrabajoRepository ordenRepo, ClienteRepository clienteRepo, UsuarioRepository usuarioRepo,
                        ServicioTarifarioRepository servicioRepo, DetalleOrdenRepository detalleRepo,
                        ComprobantePagoRepository comprobanteRepo, CorrelativoRepository correlativoRepo) {
        this.ordenRepo = ordenRepo;
        this.clienteRepo = clienteRepo;
        this.usuarioRepo = usuarioRepo;
        this.servicioRepo = servicioRepo;
        this.detalleRepo = detalleRepo;
        this.comprobanteRepo = comprobanteRepo;
        this.correlativoRepo = correlativoRepo;
    }

    // ---------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public List<OrdenListadoDTO> listar(String estado, String texto) {
        return ordenRepo.listar(estado == null ? "" : estado.trim(), texto == null ? "" : texto.trim());
    }

    @Transactional(readOnly = true)
    public OrdenTrabajo buscar(Integer idOt) {
        return ordenRepo.findById(idOt).orElseThrow(() -> new NegocioException("La orden de trabajo no existe"));
    }

    @Transactional(readOnly = true)
    public List<OrdenDetalleDTO> detalle(Integer idOt) {
        return detalleRepo.obtenerDetalle(idOt);
    }

    /** Tercer nivel de la cascada: las líneas de la OT como records JSON. */
    @Transactional(readOnly = true)
    public List<LineaDetalleDTO> detalleJson(Integer idOt) {
        return detalleRepo.obtenerDetalle(idOt).stream()
                .map(d -> new LineaDetalleDTO(d.getFormato(), d.getCategoria(), d.getCantidad(), d.getPrecioUnitario(),
                        d.getLimpieza() != null && d.getLimpieza() == 1, d.getHorasReales(), d.getPesoGb(),
                        d.getMedioEntrega(), d.getTecnico(), d.getSubtotal()))
                .toList();
    }

    /** Segundo nivel de la cascada: las OTs de un cliente. */
    @Transactional(readOnly = true)
    public List<OrdenResumenDTO> ordenesDeCliente(Integer idCliente) {
        return ordenRepo.findByCliente_IdClienteOrderByFechaRecepcionDesc(idCliente).stream()
                .map(o -> new OrdenResumenDTO(o.getIdOt(), o.getCodigoOt(), o.getFechaRecepcion().format(FECHA),
                        o.getEstado(), o.getMontoEstimado(),
                        o.getDetalles().stream().map(d -> d.getServicio().getFormato()).collect(Collectors.joining(", "))))
                .toList();
    }

    /** Precarga el formulario en modo edición. */
    @Transactional(readOnly = true)
    public List<LineaEdicionDTO> lineasParaEdicion(Integer idOt) {
        OrdenTrabajo ot = buscar(idOt);
        return ot.getDetalles().stream()
                .map(d -> new LineaEdicionDTO(d.getServicio().getIdServicio(), d.getServicio().getCategoria().getIdCategoria(),
                        d.getServicio().getFormato(), d.getServicio().getTipoCobro(), d.getPrecioUnitario(),
                        d.getCantidadMateriales(), d.isConLimpieza(), d.getServicio().isAdmiteLimpieza()))
                .toList();
    }

    // ---------------------------------------------------------------- registrar / editar

    /** RF-04: registra la OT. El código OT-1042 lo entrega SP_Obtener_Codigo dentro de esta misma transacción. */
    @Transactional
    public OrdenTrabajo registrar(OrdenRequest req, Integer idEjecutivo) {
        Cliente cliente = validarCliente(req);
        Usuario ejecutivo = usuarioRepo.findById(idEjecutivo).orElseThrow(() -> new NegocioException("El usuario no existe"));

        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setCodigoOt(correlativoRepo.obtenerCodigo("OT"));
        ot.setCliente(cliente);
        ot.setEjecutivo(ejecutivo);
        ot.setFechaRecepcion(LocalDateTime.now());
        ot.setEstado(OrdenTrabajo.EN_RECEPCION);
        armarDetalles(ot, req.lineas(), new HashSet<>());
        return ordenRepo.save(ot);
    }

    /** RF-05 / RF-06: solo se edita una OT En Recepción que el técnico aún no capturó. */
    @Transactional
    public OrdenTrabajo actualizar(Integer idOt, OrdenRequest req) {
        OrdenTrabajo ot = buscar(idOt);
        if (!ot.isEditable()) {
            throw new NegocioException("La OT " + ot.getCodigoOt() + " ya no se puede editar (estado: " + ot.getEstado()
                    + " o con captura técnica registrada)");
        }
        Cliente cliente = validarCliente(req);
        Set<Integer> yaUsados = ot.getDetalles().stream().map(d -> d.getServicio().getIdServicio()).collect(Collectors.toSet());
        ot.setCliente(cliente);
        ot.getDetalles().clear();               // orphanRemoval elimina las líneas anteriores
        armarDetalles(ot, req.lineas(), yaUsados);
        return ordenRepo.save(ot);
    }

    private Cliente validarCliente(OrdenRequest req) {
        if (req == null || req.idCliente() == null) {
            throw new NegocioException("Seleccione el cliente de la OT");
        }
        return clienteRepo.findById(req.idCliente()).orElseThrow(() -> new NegocioException("El cliente no existe"));
    }

    private void armarDetalles(OrdenTrabajo ot, List<LineaRequest> lineas, Set<Integer> serviciosPermitidosInactivos) {
        if (lineas == null || lineas.isEmpty()) {
            throw new NegocioException("Agregue al menos un formato a la OT");
        }
        if (lineas.size() > MAX_LINEAS) {
            throw new NegocioException("Una OT admite como máximo " + MAX_LINEAS + " líneas");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (LineaRequest l : lineas) {
            if (l.idServicio() == null) {
                throw new NegocioException("Seleccione el formato en todas las líneas");
            }
            if (l.cantidad() == null || l.cantidad() < 1 || l.cantidad() > MAX_CANTIDAD) {
                throw new NegocioException("La cantidad debe estar entre 1 y " + MAX_CANTIDAD);
            }
            ServicioTarifario s = servicioRepo.findById(l.idServicio())
                    .orElseThrow(() -> new NegocioException("El servicio seleccionado no existe"));
            if (!"ACTIVO".equals(s.getEstado()) && !serviciosPermitidosInactivos.contains(s.getIdServicio())) {
                throw new NegocioException("El formato " + s.getFormato() + " está inactivo en el tarifario");
            }
            boolean limpieza = Boolean.TRUE.equals(l.limpieza()) && s.isAdmiteLimpieza();

            DetalleOrden d = new DetalleOrden();
            d.setServicio(s);
            d.setCantidadMateriales(l.cantidad());
            d.setPrecioUnitario(s.getPrecioUnitario());           // tarifa vigente al recibir la OT
            d.setSubtotal(calcularSubtotal(s, s.getPrecioUnitario(), l.cantidad(), limpieza, null));
            if (limpieza) {
                MantenimientoCinta m = new MantenimientoCinta();
                m.setDetalle(d);
                m.setAplicaLimpieza(Boolean.TRUE);
                m.setCostoAdicional(COSTO_LIMPIEZA);
                d.setMantenimiento(m);
            }
            ot.agregarDetalle(d);
            total = total.add(d.getSubtotal());
        }
        ot.setMontoEstimado(total.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Regla de cobro del Avance 2 (RF-03 / RF-08):
     *  - Video con horas reales capturadas -> precio x horas reales
     *  - cualquier otro caso               -> precio x cantidad (unidades u horas estimadas)
     *  - más S/ 15.00 si la línea lleva limpieza.
     */
    public static BigDecimal calcularSubtotal(ServicioTarifario servicio, BigDecimal precio, int cantidad,
                                              boolean limpieza, BigDecimal horasReales) {
        BigDecimal unidades = (servicio.isEsVideo() && horasReales != null && horasReales.signum() > 0)
                ? horasReales
                : BigDecimal.valueOf(cantidad);
        BigDecimal subtotal = precio.multiply(unidades);
        if (limpieza && servicio.isAdmiteLimpieza()) {
            subtotal = subtotal.add(COSTO_LIMPIEZA);
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- cancelar / cobrar

    @Transactional
    public void cancelar(Integer idOt) {
        OrdenTrabajo ot = buscar(idOt);
        if (OrdenTrabajo.FINALIZADO.equals(ot.getEstado())) {
            throw new NegocioException("La OT " + ot.getCodigoOt() + " ya fue cobrada y entregada: no se puede cancelar");
        }
        if (OrdenTrabajo.CANCELADO.equals(ot.getEstado())) {
            throw new NegocioException("La OT " + ot.getCodigoOt() + " ya estaba cancelada");
        }
        ot.setEstado(OrdenTrabajo.CANCELADO);
        ordenRepo.save(ot);
    }

    /** Cobro y entrega: SP_Liquidar_Orden genera la boleta y finaliza la OT en una sola transacción. */
    @Transactional
    public ComprobantePago liquidar(Integer idOt) {
        OrdenTrabajo ot = buscar(idOt);
        if (!OrdenTrabajo.POR_NOTIFICAR.equals(ot.getEstado())) {
            throw new NegocioException("Solo se puede cobrar una OT en estado Por Notificar (la OT está " + ot.getEstado() + ")");
        }
        try {
            ordenRepo.liquidarOrden(idOt);
        } catch (DataAccessException e) {
            Throwable causa = e.getMostSpecificCause();
            throw new NegocioException("No se pudo cobrar la OT: " + (causa == null ? e.getMessage() : causa.getMessage()));
        }
        return comprobanteRepo.findByOt_IdOt(idOt).orElseThrow(() -> new NegocioException("No se generó el comprobante"));
    }

    // ---------------------------------------------------------------- comprobante (boleta)

    /** Arma los datos de la boleta: el monto cobrado ya incluye IGV, por eso subtotal = total / 1.18. */
    @Transactional(readOnly = true)
    public ComprobanteDTO armarComprobante(Integer idOt) {
        OrdenTrabajo ot = buscar(idOt);
        ComprobantePago cp = comprobanteRepo.findByOt_IdOt(idOt)
                .orElseThrow(() -> new NegocioException("La OT " + ot.getCodigoOt() + " aún no tiene comprobante (se emite al cobrar)"));

        BigDecimal total = cp.getMontoFinal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);

        ComprobanteDTO dto = new ComprobanteDTO();
        dto.setNumero(cp.getNumeroComprobante());
        dto.setFechaEmision(cp.getFechaPago().format(FECHA_HORA));
        dto.setCodigoOt(ot.getCodigoOt());
        dto.setCliente(ot.getCliente().getNombres());
        dto.setDocumento(ot.getCliente().getDniCliente());
        dto.setCorreoCliente(ot.getCliente().getCorreo());
        dto.setEjecutivo(ot.getEjecutivo().getNombres());
        dto.setTotal(total);
        dto.setSubtotal(subtotal);
        dto.setIgv(total.subtract(subtotal));
        dto.setLineas(detalleRepo.obtenerDetalle(idOt));
        return dto;
    }
}
