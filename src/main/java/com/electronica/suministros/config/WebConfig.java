package com.electronica.suministros.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el interceptor de seguridad solo sobre las rutas privadas.
 * Todo lo que no esté en esta lista (/, /servicios, /contacto, /login, /assets/**,
 * /api/catalogo/**) es público.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * Las vistas piden las fotos como /assets/img/xxx.jpg. Se buscan primero en static/assets/img/
     * y, si no están, en static/assets/ (donde estaban en Avance 2). Así funciona con cualquiera
     * de las dos carpetas, sin mover archivos.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/img/**")
                .addResourceLocations("classpath:/static/assets/img/", "classpath:/static/assets/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/admin/**",
                        "/ejecutivo/**",
                        "/tecnico/**",
                        "/api/ordenes/**",
                        "/api/reportes/**");
    }
}
