package com.electronica.suministros.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.electronica.suministros.service.OrdenService;
import com.electronica.suministros.service.ReporteService;

/** Pantallas gerenciales (solo ADMIN): dashboard con KPIs y reportes de ingresos. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReporteService reporteService;
    private final OrdenService ordenService;

    public AdminController(ReporteService reporteService, OrdenService ordenService) {
        this.reporteService = reporteService;
        this.ordenService = ordenService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("kpi", reporteService.kpis());
        model.addAttribute("recientes", ordenService.listar("", "").stream().limit(6).toList());
        model.addAttribute("anio", LocalDate.now().getYear());
        return "intranet/admin/dashboard";
    }

    @GetMapping("/reportes")
    public String reportes(Model model) {
        int anio = LocalDate.now().getYear();
        model.addAttribute("anios", reporteService.aniosDisponibles());
        model.addAttribute("anio", anio);
        model.addAttribute("formatos", reporteService.ingresosPorFormato(anio));
        return "intranet/admin/reportes";
    }
}
