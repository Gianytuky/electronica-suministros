package com.electronica.suministros.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía el correo de aviso al cliente cuando su OT pasa a "Por Notificar".
 *
 * Garantías que pide el requerimiento:
 *  - @TransactionalEventListener (fase por defecto AFTER_COMMIT): el correo se envía SOLO si la
 *    captura técnica ya quedó guardada en la base de datos. Si la transacción falla, no se avisa a nadie.
 *  - @Async: el envío corre en otro hilo; el técnico no espera al servidor SMTP.
 *  - try/catch: cualquier fallo del correo se registra en el log y se descarta. Como la transacción
 *    ya terminó, un error aquí NUNCA puede revertir la captura técnica.
 */
@Component
public class NotificacionOrdenListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionOrdenListener.class);

    private final EmailService emailService;

    public NotificacionOrdenListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener
    public void alPasarAPorNotificar(OrdenPorNotificarEvent evento) {
        if (evento.correoCliente() == null || evento.correoCliente().isBlank()) {
            log.info("La OT {} pasó a Por Notificar, pero el cliente no tiene correo registrado: no se envió aviso.",
                    evento.codigoOt());
            return;
        }
        try {
            emailService.avisoOrdenLista(evento.correoCliente(), evento.cliente(), evento.codigoOt(),
                    evento.medios(), evento.monto());
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar por correo la OT {} a {}: {}", evento.codigoOt(), evento.correoCliente(),
                    e.getMessage());
        }
    }
}
