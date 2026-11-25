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
    private final EmailService emailService;

    public MensajeContactoService(MensajeContactoRepository mensajeRepo, EmailService emailService) {
        this.mensajeRepo = mensajeRepo;
        this.emailService = emailService;
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

    /**
     * Avance final: el Ejecutivo responde una consulta directamente por correo desde la bandeja.
     * El envío es síncrono a propósito (para avisarle al instante si falló) y por eso el método no es
     * @Transactional: no se mantiene abierta una transacción mientras se espera al servidor SMTP.
     * Si el correo sale bien, la consulta se marca como LEIDO. Devuelve el destino para mostrarlo en pantalla:
     * el correo del cliente o, con el modo prueba activo, la cuenta de pruebas junto al destinatario real.
     */
    public String responder(Integer idMensaje, String asunto, String respuesta, String ejecutivo) {
        MensajeContacto m = mensajeRepo.findById(idMensaje)
                .orElseThrow(() -> new NegocioException("La consulta ya no existe en la bandeja."));
        if (asunto == null || asunto.isBlank() || respuesta == null || respuesta.isBlank()) {
            throw new NegocioException("Escriba el asunto y el texto de la respuesta.");
        }
        if (asunto.trim().length() > 150) {
            throw new NegocioException("El asunto no puede superar los 150 caracteres.");
        }
        if (respuesta.trim().length() > 3000) {
            throw new NegocioException("La respuesta no puede superar los 3000 caracteres.");
        }
        try {
            emailService.respuestaConsulta(m.getCorreo(), m.getNombre(), asunto.trim(), respuesta,
                    m.getMensaje(), ejecutivo);
        } catch (CorreoException e) {
            throw new NegocioException("No se pudo enviar la respuesta. Revise la configuración de correo "
                    + "(MAIL_USER / MAIL_PASSWORD) y que el correo del cliente sea válido. Detalle: " + e.getMessage());
        }
        m.setEstado("LEIDO");
        mensajeRepo.save(m);
        return emailService.describirDestino(m.getCorreo());
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
