package com.electronica.suministros;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SuministrosApplication {

    public static void main(String[] args) {
        // Render ejecuta en UTC; fijamos la hora de Lima para que las fechas de
        // OTs, boletas y mensajes coincidan con la hora local del negocio.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        SpringApplication.run(SuministrosApplication.class, args);
    }
}
