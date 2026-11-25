package com.electronica.suministros.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita @Async para que los correos automáticos no bloqueen la petición del usuario. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
