package com.electronica.suministros.service;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.electronica.suministros.config.ClienteHttp;
import com.electronica.suministros.dto.ReniecDTO;
import com.electronica.suministros.dto.RucDTO;

import tools.jackson.databind.JsonNode;

/**
 * Avance final (integraciones): consulta de DNI (RENIEC) y de RUC (padrón de SUNAT) mediante ApiPeru.dev.
 *
 * Igual que el ejemplo de clase: POST https://apiperu.dev/api/dni con {"dni": "..."} y
 * POST https://apiperu.dev/api/ruc con {"ruc": "..."}, ambos con la cabecera Authorization: Bearer token.
 * Diferencias por seguridad:
 *  - El token no está en el código: se lee de APIPERU_TOKEN (.env o variables de entorno), igual que MAIL_*.
 *  - Los endpoints propios /api/reniec/** están protegidos por AuthInterceptor (solo EJECUTIVO y ADMIN) y no usan
 *    @CrossOrigin, para que nadie de fuera gaste la cuota del plan gratuito ni consulte datos personales.
 *  - Se valida el formato (8 dígitos para DNI, 11 para RUC) antes de llamar a la API.
 * Los errores se lanzan como ResponseStatusException; ReniecRestController los devuelve como {"error": "..."}.
 */
@Service
public class ReniecService {

    private static final Logger log = LoggerFactory.getLogger(ReniecService.class);
    private static final Pattern DNI = Pattern.compile("\\d{8}");
    private static final Pattern RUC = Pattern.compile("\\d{11}");
    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");

    private final RestClient http = ClienteHttp.crear(Duration.ofSeconds(3), Duration.ofSeconds(6));
    private final String token;
    private final String urlDni;
    private final String urlRuc;

    public ReniecService(@Value("${app.apiperu.token:}") String token,
                         @Value("${app.apiperu.url-dni:https://apiperu.dev/api/dni}") String urlDni,
                         @Value("${app.apiperu.url-ruc:https://apiperu.dev/api/ruc}") String urlRuc) {
        this.token = token == null ? "" : token.trim();
        this.urlDni = urlDni;
        this.urlRuc = urlRuc;
    }

    /** DNI (8 dígitos) -> nombres y apellidos según RENIEC. */
    public ReniecDTO consultar(String dniIngresado) {
        String dni = dniIngresado == null ? "" : dniIngresado.trim();
        if (!DNI.matcher(dni).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El DNI debe tener 8 dígitos.");
        }
        JsonNode data = consultarApi(urlDni, "dni", dni, "el DNI");
        String nombres = texto(data.path("nombres"));
        if (nombres.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el DNI " + dni + " en RENIEC.");
        }
        String paterno = texto(data.path("apellido_paterno"));
        String materno = texto(data.path("apellido_materno"));
        String completo = texto(data.path("nombre_completo"));
        String paraCliente = recortar100(titulo(String.join(" ", nombres, paterno, materno)));
        return new ReniecDTO(dni, nombres, paterno, materno, completo, paraCliente);
    }

    /** RUC (11 dígitos) -> razón social, estado y condición según el padrón de SUNAT. */
    public RucDTO consultarRuc(String rucIngresado) {
        String ruc = rucIngresado == null ? "" : rucIngresado.trim();
        if (!RUC.matcher(ruc).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RUC debe tener 11 dígitos.");
        }
        JsonNode data = consultarApi(urlRuc, "ruc", ruc, "el RUC");
        String razonSocial = texto(data.path("nombre_o_razon_social"));
        if (razonSocial.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el RUC " + ruc + " en SUNAT.");
        }
        // La razón social se conserva como la registra SUNAT (mayúsculas), para no alterar siglas como S.A.C.
        return new RucDTO(ruc, razonSocial,
                texto(data.path("estado")),
                texto(data.path("condicion")),
                texto(data.path("direccion")),
                texto(data.path("departamento")),
                recortar100(razonSocial));
    }

    /** POST a ApiPeru.dev con {campo: valor}; devuelve el nodo "data" o lanza un error HTTP claro. */
    private JsonNode consultarApi(String url, String campo, String valor, String nombreDoc) {
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La consulta a ApiPeru.dev no está configurada: falta APIPERU_TOKEN en el archivo .env.");
        }
        JsonNode raiz;
        try {
            raiz = http.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of(campo, valor))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            int estado = e.getStatusCode().value();
            log.warn("ApiPeru.dev respondió HTTP {} al consultar {}.", estado, nombreDoc);
            if (estado == 401 || estado == 403) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "ApiPeru.dev rechazó el token configurado (revise APIPERU_TOKEN).");
            }
            if (estado == 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "ApiPeru.dev indica que " + nombreDoc + " " + valor + " es incorrecto.");
            }
            if (estado == 404 || estado == 422) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró " + nombreDoc + " " + valor + ".");
            }
            if (estado == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Se agotó la cuota de consultas de ApiPeru.dev. Escriba los datos a mano.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "ApiPeru.dev no está disponible en este momento (HTTP " + estado + "). Intente nuevamente.");
        } catch (RuntimeException e) {
            log.warn("No se pudo conectar con ApiPeru.dev para consultar {}: {}", nombreDoc, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con ApiPeru.dev. Intente nuevamente o escriba los datos a mano.");
        }
        JsonNode exito = raiz == null ? null : raiz.path("success");
        JsonNode data = raiz == null ? null : raiz.path("data");
        if (exito == null || (exito.isBoolean() && !exito.booleanValue()) || data == null || !data.isObject()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró " + nombreDoc + " " + valor + ".");
        }
        return data;
    }

    private static String texto(JsonNode nodo) {
        if (nodo == null || nodo.isMissingNode() || nodo.isNull() || nodo.isContainer()) {
            return "";
        }
        return nodo.asString().trim();
    }

    private static String recortar100(String s) {
        return s.length() > 100 ? s.substring(0, 100).trim() : s;
    }

    /** "MAXIMO ALBERTO MORENO CUEVA" -> "Maximo Alberto Moreno Cueva" (mismo estilo que los clientes registrados). */
    private static String titulo(String texto) {
        StringBuilder sb = new StringBuilder();
        for (String palabra : texto.trim().toLowerCase(ES_PE).split("\\s+")) {
            if (palabra.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(palabra.substring(0, 1).toUpperCase(ES_PE)).append(palabra.substring(1));
        }
        return sb.toString();
    }
}
