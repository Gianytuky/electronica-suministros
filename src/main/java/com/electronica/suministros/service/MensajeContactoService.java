package com.electronica.suministros.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.entity.MensajeContacto;
import com.electronica.suministros.repository.MensajeContactoRepository;

@Service
public class MensajeContactoService {

    private final MensajeContactoRepository mensajeRepo;

    public MensajeContactoService(MensajeContactoRepository mensajeRepo) {
        this.mensajeRepo = mensajeRepo;
    }

    @Transactional
    public void registrar(String nombre, String correo, String telefono, String mensaje) {
        if (nombre == null || nombre.isBlank() || correo == null || correo.isBlank()
                || mensaje == null || mensaje.isBlank()) {
            throw new NegocioException("Complete su nombre, correo y mensaje");
        }
        if (mensaje.length() > 1000) {
            throw new NegocioException("El mensaje no puede superar los 1000 caracteres");
        }
        MensajeContacto m = new MensajeContacto();
        m.setNombre(nombre.trim());
        m.setCorreo(correo.trim());
        m.setTelefono(telefono == null || telefono.isBlank() ? null : telefono.trim());
        m.setMensaje(mensaje.trim());
        m.setFechaEnvio(LocalDateTime.now());
        m.setEstado("NUEVO");
        mensajeRepo.save(m);
    }

    /** Bandeja filtrada por rango de fechas (LocalDate), como en el Avance 2. */
    @Transactional(readOnly = true)
    public List<MensajeContacto> listar(LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = (desde == null) ? LocalDate.of(2000, 1, 1).atStartOfDay() : desde.atStartOfDay();
        LocalDateTime fin = (hasta == null) ? LocalDate.of(2100, 12, 31).atTime(23, 59, 59) : hasta.atTime(23, 59, 59);
        return mensajeRepo.findByFechaEnvioBetweenOrderByFechaEnvioDesc(ini, fin);
    }

    @Transactional
    public void marcarLeido(Integer idMensaje) {
        mensajeRepo.findById(idMensaje).ifPresent(m -> {
            m.setEstado("LEIDO");
            mensajeRepo.save(m);
        });
    }

    @Transactional
    public void eliminar(Integer idMensaje) {
        mensajeRepo.deleteById(idMensaje);
    }

    @Transactional(readOnly = true)
    public long contarNuevos() {
        return mensajeRepo.countByEstado("NUEVO");
    }
}
