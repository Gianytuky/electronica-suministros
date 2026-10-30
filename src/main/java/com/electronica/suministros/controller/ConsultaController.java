package com.electronica.suministros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Página de consulta en cascada Cliente -> OT -> Detalle (los datos llegan por fetch desde /api/ordenes). */
@Controller
@RequestMapping("/ejecutivo/consulta")
public class ConsultaController {

    @GetMapping
    public String consulta() {
        return "intranet/ejecutivo/consulta_ordenes";
    }
}
