package com.electronica.suministros.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.electronica.suministros.config.ClienteHttp;
import com.electronica.suministros.dto.TipoCambioDTO;

import tools.jackson.databind.JsonNode;

/**
 * Avance final (integraciones): tipo de cambio referencial USD -> PEN desde ApiPeru.dev.
 *
 * Reglas:
 *  - Caché en memoria: un valor bueno se reutiliza durante app.tipo-cambio.horas-cache (6 h), así el
 *    plan gratuito de ApiPeru.dev se consume como máximo unas 4 veces al día y la navegación no se ralentiza.
 *  - La respuesta se lee de forma defensiva con Jackson (JsonNode: data.venta, data.compra, data.fecha).
 *    Si falta un campo, cambia el formato o la API no responde, se sigue mostrando el último valor bueno
 *    (con su fecha) o, si nunca hubo uno, el valor de respaldo configurado (app.tipo-cambio.respaldo).
 *  - Tras un fallo se espera app.tipo-cambio.minutos-reintento (60 min) antes de volver a consultar;
 *    si el token es rechazado (HTTP 401/403), se espera 24 h. Este método nunca lanza excepciones.
 *  - Sin APIPERU_TOKEN no se llama a la API: se usa directamente el valor de respaldo.
 *
 * Se usa desde fragments/header.html con ${@tipoCambioService.actual()} y desde OrdenController (boleta PDF).
 */
@Service
public class TipoCambioService {

    private static final Logger log = LoggerFactory.getLogger(TipoCambioService.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RestClient http = ClienteHttp.crear(Duration.ofSeconds(2), Duration.ofSeconds(4));
    private final String token;
    private final String url;
    private final Duration vigencia;
    private final Duration esperaTrasFallo;
    private final BigDecimal respaldo;

    private volatile TipoCambioDTO ultimoValido;
    private volatile Instant proximaConsulta = Instant.EPOCH;
    private volatile boolean tokenRechazado;

    public TipoCambioService(@Value("${app.apiperu.token:}") String token,
                             @Value("${app.apiperu.url-tipo-cambio:https://apiperu.dev/api/tipo-de-cambio}") String url,
                             @Value("${app.tipo-cambio.horas-cache:6}") long horasCache,
                             @Value("${app.tipo-cambio.minutos-reintento:60}") long minutosReintento,
                             @Value("${app.tipo-cambio.respaldo:3.75}") BigDecimal respaldo) {
        this.token = token == null ? "" : token.trim();
        this.url = url;
        this.vigencia = Duration.ofHours(Math.max(1, horasCache));
        this.esperaTrasFallo = Duration.ofMinutes(Math.max(5, minutosReintento));
        this.respaldo = respaldo;
    }

    /** Tipo de cambio para mostrar: el de la caché, uno recién consultado o el de respaldo. Nunca falla. */
    public TipoCambioDTO actual() {
        if (Instant.now().isBefore(proximaConsulta)) {
            return valorDisponible();
        }
        synchronized (this) {
            if (Instant.now().isBefore(proximaConsulta)) {
                return valorDisponible();
            }
            if (token.isEmpty()) {
                log.warn("APIPERU_TOKEN no está configurado: se muestra el tipo de cambio de respaldo S/ {}.", respaldo);
                proximaConsulta = Instant.now().plus(vigencia);
                return valorDisponible();
            }
            tokenRechazado = false;
            TipoCambioDTO nuevo = consultarApi();
            if (nuevo != null) {
                ultimoValido = nuevo;
                proximaConsulta = Instant.now().plus(vigencia);
            } else {
                proximaConsulta = Instant.now().plus(tokenRechazado ? Duration.ofHours(24) : esperaTrasFallo);
            }
            return valorDisponible();
        }
    }

    private TipoCambioDTO valorDisponible() {
        TipoCambioDTO tc = ultimoValido;
        if (tc != null) {
            return tc;
        }
        LocalDateTime ahora = LocalDateTime.now(LIMA);
        return new TipoCambioDTO(respaldo, respaldo, ahora.format(FECHA), ahora.format(FECHA_HORA), true);
    }

    /** POST https://apiperu.dev/api/tipo-de-cambio con {"fecha": "aaaa-mm-dd"} (como en el ejemplo de clase). */
    private TipoCambioDTO consultarApi() {
        LocalDate hoy = LocalDate.now(LIMA);
        try {
            JsonNode raiz = http.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("fecha", hoy.toString()))
                    .retrieve()
                    .body(JsonNode.class);
            if (raiz == null) {
                log.warn("ApiPeru.dev devolvió una respuesta vacía al consultar el tipo de cambio.");
                return null;
            }
            JsonNode exito = raiz.path("success");
            if (exito.isBoolean() && !exito.booleanValue()) {
                log.warn("ApiPeru.dev no tiene tipo de cambio para {}: {}", hoy, recortar(raiz.toString()));
                return null;
            }
            JsonNode data = raiz.path("data");
            BigDecimal venta = numero(data.path("venta"));
            BigDecimal compra = numero(data.path("compra"));
            if (venta == null) {
                log.warn("ApiPeru.dev respondió sin un data.venta válido: {}", recortar(raiz.toString()));
                return null;
            }
            if (compra == null) {
                compra = venta;
            }
            String fecha = fechaLegible(texto(data.path("fecha")), hoy);
            log.info("Tipo de cambio actualizado desde ApiPeru.dev: venta S/ {} del {} (se reutiliza {} h).",
                    venta, fecha, vigencia.toHours());
            return new TipoCambioDTO(compra, venta, fecha, LocalDateTime.now(LIMA).format(FECHA_HORA), false);
        } catch (RestClientResponseException e) {
            int estado = e.getStatusCode().value();
            if (estado == 401 || estado == 403) {
                tokenRechazado = true;
                log.error("ApiPeru.dev rechazó el token (HTTP {}). Revise APIPERU_TOKEN; se reintentará en 24 h.", estado);
            } else {
                log.warn("ApiPeru.dev respondió HTTP {} al consultar el tipo de cambio.", estado);
            }
            return null;
        } catch (RuntimeException e) {
            log.warn("No se pudo consultar el tipo de cambio en ApiPeru.dev: {}", e.getMessage());
            return null;
        }
    }

    /** Lee un número aunque venga como texto ("3.751") o como número (3.751). Fuera de 1 a 10 se descarta. */
    private static BigDecimal numero(JsonNode nodo) {
        String valor = texto(nodo);
        if (valor.isEmpty()) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(valor.replace(",", "."));
            return (v.compareTo(BigDecimal.ONE) >= 0 && v.compareTo(BigDecimal.TEN) <= 0) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String texto(JsonNode nodo) {
        if (nodo == null || nodo.isMissingNode() || nodo.isNull() || nodo.isContainer()) {
            return "";
        }
        return nodo.asString().trim();
    }

    /** "2026-11-18" -> "18/11/2026"; si viene en otro formato se muestra tal cual; si no viene, la fecha de hoy. */
    private static String fechaLegible(String fecha, LocalDate hoy) {
        if (fecha.isEmpty()) {
            return hoy.format(FECHA);
        }
        try {
            return LocalDate.parse(fecha.length() > 10 ? fecha.substring(0, 10) : fecha).format(FECHA);
        } catch (RuntimeException e) {
            return fecha;
        }
    }

    private static String recortar(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
