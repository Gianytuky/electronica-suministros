package com.electronica.suministros.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.dto.CapturaFormDTO;
import com.electronica.suministros.dto.CapturaRecienteDTO;
import com.electronica.suministros.dto.OrdenListadoDTO;
import com.electronica.suministros.entity.CapturaTecnica;
import com.electronica.suministros.entity.DetalleOrden;
import com.electronica.suministros.entity.OrdenTrabajo;
import com.electronica.suministros.entity.Usuario;
import com.electronica.suministros.repository.CapturaTecnicaRepository;
import com.electronica.suministros.repository.DetalleOrdenRepository;
import com.electronica.suministros.repository.OrdenTrabajoRepository;
import com.electronica.suministros.repository.UsuarioRepository;

/**
 * Panel del técnico (RF-07 y RF-08): registra horas reales, peso en GB y medio de entrega de cada línea,
 * recalcula el subtotal y, cuando todas las líneas de la OT fueron capturadas, la pasa a "Por Notificar".
 */
@Service
public class CapturaService {

    public static final List<String> MEDIOS_ENTREGA = List.of(
            "Memoria USB (32 GB)",
            "Memoria USB (64 GB)",
            "Disco Duro Externo (1 TB)",
            "Enlace Nube (Google Drive / OneDrive)");

    private final CapturaTecnicaRepository capturaRepo;
    private final DetalleOrdenRepository detalleRepo;
    private final OrdenTrabajoRepository ordenRepo;
    private final UsuarioRepository usuarioRepo;
    private final ApplicationEventPublisher eventos;

    public CapturaService(CapturaTecnicaRepository capturaRepo, DetalleOrdenRepository detalleRepo,
                          OrdenTrabajoRepository ordenRepo, UsuarioRepository usuarioRepo,
                          ApplicationEventPublisher eventos) {
        this.capturaRepo = capturaRepo;
        this.detalleRepo = detalleRepo;
        this.ordenRepo = ordenRepo;
        this.usuarioRepo = usuarioRepo;
        this.eventos = eventos;
    }

    /** Cola de trabajo: OTs que esperan captura (estado En Recepción). */
    @Transactional(readOnly = true)
    public List<OrdenListadoDTO> cola() {
        return ordenRepo.listar(OrdenTrabajo.EN_RECEPCION, "");
    }

    @Transactional(readOnly = true)
    public List<CapturaRecienteDTO> ultimas() {
        return capturaRepo.ultimas();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalHoras() {
        BigDecimal total = capturaRepo.sumarHoras();
        return total == null ? BigDecimal.ZERO : total;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalGb() {
        BigDecimal total = capturaRepo.sumarGb();
        return total == null ? BigDecimal.ZERO : total;
    }

    /** Devuelve la OT actualizada para que el controlador muestre el mensaje de confirmación. */
    @Transactional
    public OrdenTrabajo registrar(CapturaFormDTO f, Integer idTecnico) {
        DetalleOrden d = detalleRepo.findById(f.getIdDetalle()).orElseThrow(() -> new NegocioException("La línea de la OT no existe"));
        OrdenTrabajo ot = d.getOt();
        if (!OrdenTrabajo.EN_RECEPCION.equals(ot.getEstado())) {
            throw new NegocioException("La OT " + ot.getCodigoOt() + " está " + ot.getEstado() + " y ya no admite captura técnica");
        }
        if (!MEDIOS_ENTREGA.contains(f.getMedioEntrega())) {
            throw new NegocioException("Seleccione un medio de entrega válido");
        }
        Usuario tecnico = usuarioRepo.findById(idTecnico).orElseThrow(() -> new NegocioException("El técnico no existe"));

        CapturaTecnica c = d.getCaptura();
        if (c == null) {
            c = new CapturaTecnica();
            c.setDetalle(d);
            d.setCaptura(c);
        }
        c.setTecnico(tecnico);
        c.setDuracionHorasReales(f.getHorasReales());
        c.setPesoGb(f.getPesoGb());
        c.setMedioEntrega(f.getMedioEntrega());
        c.setFechaCaptura(LocalDateTime.now());
        capturaRepo.save(c);

        // RF-08: recálculo del precio exacto con la medición real
        d.setSubtotal(OrdenService.calcularSubtotal(d.getServicio(), d.getPrecioUnitario(), d.getCantidadMateriales(),
                d.isConLimpieza(), f.getHorasReales()));
        detalleRepo.save(d);

        BigDecimal total = ot.getDetalles().stream().map(DetalleOrden::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        ot.setMontoEstimado(total);
        boolean pasaAPorNotificar = ot.getDetalles().stream().allMatch(x -> x.getCaptura() != null);
        if (pasaAPorNotificar) {
            ot.setEstado(OrdenTrabajo.POR_NOTIFICAR);
        }
        OrdenTrabajo guardada = ordenRepo.save(ot);
        if (pasaAPorNotificar) {
            // Avance final: aviso automático por correo al cliente. El listener lo envía DESPUÉS del commit
            // y en otro hilo, así que si el correo falla la captura técnica no se revierte (ver NotificacionOrdenListener).
            List<String> medios = guardada.getDetalles().stream()
                    .map(x -> x.getCaptura().getMedioEntrega())
                    .distinct()
                    .toList();
            eventos.publishEvent(new OrdenPorNotificarEvent(guardada.getCodigoOt(),
                    guardada.getCliente().getNombres(), guardada.getCliente().getCorreo(),
                    guardada.getMontoEstimado(), medios));
        }
        return guardada;
    }
}
