package com.electronica.suministros.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envío de correos con JavaMailSender (Gmail SMTP por defecto; ver application.properties).
 * Adaptado de la clase EmailService de la práctica de tokens (S13): ahora en UTF-8, con remitente
 * configurable y con las plantillas de texto que usa el sistema:
 *   1. aviso al cliente cuando su OT pasa a "Por Notificar",
 *   2. enlace de recuperación de contraseña (token UUID),
 *   3. respuesta del Ejecutivo a una consulta de la bandeja de mensajes,
 *   4. boleta electrónica en PDF adjunta (avance final).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String EMPRESA = "Electrónica y Suministros S.A.C.";

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String nombreRemitente;
    private final String redirigirA;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from:}") String remitente,
                        @Value("${app.mail.nombre-remitente:" + EMPRESA + "}") String nombreRemitente,
                        @Value("${app.mail.redirigir-a:}") String redirigirA) {
        this.mailSender = mailSender;
        this.remitente = remitente == null ? "" : remitente.trim();
        this.nombreRemitente = nombreRemitente;
        this.redirigirA = redirigirA == null ? "" : redirigirA.trim();
        if (!this.redirigirA.isEmpty()) {
            log.warn("MODO PRUEBA DE CORREO ACTIVO: todos los correos se enviarán a {} (MAIL_REDIRIGIR_A). "
                    + "Déjelo vacío en producción.", this.redirigirA);
        }
    }

    // ------------------------------------------------------------------ modo prueba

    /** true si el modo prueba (MAIL_REDIRIGIR_A) está activo: todo el correo se desvía a una sola cuenta. */
    public boolean isModoPrueba() {
        return !redirigirA.isEmpty();
    }

    /**
     * Texto para los mensajes en pantalla que dicen a dónde va el correo. En producción es el correo del
     * destinatario; con el modo prueba activo aclara que la entrega real va a la cuenta de pruebas.
     */
    public String describirDestino(String correoReal) {
        String real = correoReal == null ? "" : correoReal.trim();
        if (!isModoPrueba()) {
            return real;
        }
        return redirigirA + " (modo prueba; destinatario real: " + (real.isEmpty() ? "sin correo" : real) + ")";
    }

    // ------------------------------------------------------------------ envío base

    /** Envía un correo de texto plano. Lanza CorreoException si no se pudo enviar. */
    public void enviar(String para, String asunto, String cuerpo) {
        enviar(para, asunto, cuerpo, null, null);
    }

    /**
     * Envía un correo de texto con un PDF adjunto opcional (nombreAdjunto y pdf en null = sin adjunto).
     * Lanza CorreoException si no se pudo enviar.
     */
    public void enviar(String para, String asunto, String cuerpo, String nombreAdjunto, byte[] pdf) {
        if (para == null || para.isBlank()) {
            throw new CorreoException("El destinatario no tiene correo");
        }
        if (remitente.isBlank()) {
            throw new CorreoException("El correo no está configurado: defina MAIL_USER y MAIL_PASSWORD "
                    + "(variables de entorno o archivo .env)");
        }
        // Modo prueba: en localhost evita escribirle a clientes reales de los datos de ejemplo.
        if (!redirigirA.isEmpty()) {
            asunto = "[PRUEBA - original para " + para.trim() + "] " + asunto;
            para = redirigirA;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            boolean conAdjunto = pdf != null && nombreAdjunto != null;
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, conAdjunto, "UTF-8");
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(para.trim());
            helper.setSubject(asunto);
            helper.setText(cuerpo, false);
            if (conAdjunto) {
                helper.addAttachment(nombreAdjunto, new ByteArrayResource(pdf), "application/pdf");
            }
            mailSender.send(mensaje);
            log.info("Correo enviado a {} (asunto: {})", para, asunto);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new CorreoException("No se pudo enviar el correo a " + para + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ plantillas

    /** 1) Aviso de OT lista: "Estimado [Cliente], su orden [OT-xxxx] ya está digitalizada. Puede pasar a recoger su USB." */
    public void avisoOrdenLista(String correo, String cliente, String codigoOt, List<String> medios, BigDecimal monto) {
        String cuerpo = "Estimado(a) " + cliente + ",\n\n"
                + "Su orden " + codigoOt + " ya está digitalizada. Puede pasar a recoger " + describirMedios(medios) + ".\n\n"
                + "Monto total de la orden: S/ " + (monto == null ? "0.00" : monto.setScale(2, java.math.RoundingMode.HALF_UP)) + "\n"
                + "Al recoger su pedido, indique el código " + codigoOt + " en el mostrador.\n\n"
                + "Gracias por confiar en nosotros.\n"
                + EMPRESA + "\n";
        enviar(correo, "Su orden " + codigoOt + " está lista - " + EMPRESA, cuerpo);
    }

    /** 2) Recuperación de contraseña: enlace con el token UUID y su vigencia. */
    public void tokenRecuperacion(String correo, String nombre, String enlace, int minutos) {
        String cuerpo = "Hola " + nombre + ",\n\n"
                // EMPRESA ya termina en punto ("S.A.C."): no se agrega otro para no escribir "S.A.C.."
                + "Recibimos una solicitud para restablecer la contraseña de su cuenta en la intranet de " + EMPRESA + "\n\n"
                + "Para elegir una nueva contraseña ingrese a este enlace (vale " + minutos + " minutos y solo se puede usar una vez):\n"
                + enlace + "\n\n"
                + "Si usted no hizo esta solicitud, ignore este mensaje: su contraseña actual no cambiará.\n\n"
                + EMPRESA + "\n";
        enviar(correo, "Recuperación de contraseña - " + EMPRESA, cuerpo);
    }

    /** 3) Respuesta del Ejecutivo a una consulta recibida por el formulario de contacto. */
    public void respuestaConsulta(String correo, String nombreCliente, String asunto, String respuesta,
                                  String mensajeOriginal, String ejecutivo) {
        String cuerpo = "Estimado(a) " + nombreCliente + ":\n\n"
                + respuesta.trim() + "\n\n"
                + "Atentamente,\n"
                + ejecutivo + "\n"
                + EMPRESA + "\n\n"
                + "----------------------------------------\n"
                + "Su consulta original:\n"
                + mensajeOriginal + "\n";
        enviar(correo, asunto, cuerpo);
    }

    /** 4) Boleta electrónica: el PDF de la boleta va adjunto al correo del cliente. */
    public void boletaElectronica(String correo, String cliente, String numeroBoleta, String codigoOt,
                                  BigDecimal total, String nombreArchivo, byte[] pdf) {
        String cuerpo = "Estimado(a) " + cliente + ",\n\n"
                + "Adjuntamos su boleta de venta electrónica " + numeroBoleta + ", correspondiente a la orden "
                + codigoOt + ".\n\n"
                + "Importe total: S/ " + (total == null ? "0.00" : total.setScale(2, java.math.RoundingMode.HALF_UP)) + "\n\n"
                + "Conserve este documento como comprobante de su pago.\n"
                + "Gracias por confiar en nosotros.\n"
                + EMPRESA + "\n";
        enviar(correo, "Boleta electrónica " + numeroBoleta + " - " + EMPRESA, cuerpo, nombreArchivo, pdf);
    }

    // ------------------------------------------------------------------ apoyo

    /** Convierte los medios de entrega capturados por el técnico en una frase: "su USB", "su disco duro externo"... */
    static String describirMedios(List<String> medios) {
        List<String> frases = new ArrayList<>();
        if (medios != null) {
            for (String m : medios) {
                String t = m == null ? "" : m.toLowerCase();
                String frase;
                if (t.contains("usb")) {
                    frase = "su USB";
                } else if (t.contains("disco")) {
                    frase = "su disco duro externo";
                } else if (t.contains("nube")) {
                    frase = "el enlace de descarga en la nube";
                } else {
                    frase = "su pedido";
                }
                if (!frases.contains(frase)) {
                    frases.add(frase);
                }
            }
        }
        if (frases.isEmpty()) {
            return "su pedido";
        }
        return String.join(" y ", frases);
    }
}
