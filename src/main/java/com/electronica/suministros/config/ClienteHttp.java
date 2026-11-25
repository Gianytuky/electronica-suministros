package com.electronica.suministros.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Avance final (integraciones): fábrica de clientes HTTP para consumir APIs externas.
 *
 * Usa RestClient (el cliente síncrono recomendado desde Spring 6.1; RestTemplate está en retiro en
 * Spring Boot 4) sobre el HttpClient del JDK, con tiempos máximos de conexión y de lectura. Así, una
 * API caída (ApiPeru.dev o el microservicio Python) nunca deja colgada una página del sistema.
 * Se fuerza HTTP/1.1 para que el microservicio Python (Uvicorn) no reciba intentos de HTTP/2 sin TLS.
 */
public final class ClienteHttp {

    private ClienteHttp() {
    }

    public static RestClient crear(Duration conexion, Duration lectura) {
        HttpClient jdk = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(conexion)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(jdk);
        fabrica.setReadTimeout(lectura);
        return RestClient.builder().requestFactory(fabrica).build();
    }
}
