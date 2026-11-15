/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.reportes_ciudadanos.controller;

import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author Usuario
 */
@Controller
public class DashboardController {
    private final ReporteService reporteService;

    @Autowired
    public DashboardController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal Usuario usuario, Model model) {

        // Stats globales
        model.addAttribute("totalReportes",    reporteService.countTotal());
        model.addAttribute("pendientes",       reporteService.countPendiente());
        model.addAttribute("enProceso",        reporteService.countEnProceso());
        model.addAttribute("resueltos",        reporteService.countResuelto());

        // Feed: últimos 3 reportes enriquecidos
        model.addAttribute("ultimosReportes",  reporteService.findUltimos(3));

        // Todos los reportes para Leaflet (marcadores en el mapa)
        model.addAttribute("todosReportes",    reporteService.findAll());
        
        return "user/dashboard";
    }
}
