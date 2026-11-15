/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.reportes_ciudadanos.controller;

import com.utp.reportes_ciudadanos.model.Reporte;
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.ReporteService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author Usuario
 */
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    @Autowired
    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // ── NUEVO REPORTE ──────────────────────────────────────────────────
    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("reporte", new Reporte());
        model.addAttribute("categorias", reporteService.findAllCategorias());
        return "user/nuevo-reporte";
    }

    @PostMapping("/nuevo")
    public String nuevoSubmit(
            @Valid @ModelAttribute("reporte") Reporte reporte,
            BindingResult result,
            @AuthenticationPrincipal Usuario usuario,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("categorias", reporteService.findAllCategorias());
            return "user/nuevo-reporte";
        }

        reporteService.crear(
                reporte.getTitulo(),
                reporte.getDescripcion(),
                reporte.getCategoriaId(),
                reporte.getUbicacion(),
                reporte.getLatitud(),
                reporte.getLongitud(),
                usuario.getId());

        redirectAttributes.addFlashAttribute(
                "exito",
                "¡Reporte enviado correctamente!");

        return "redirect:/dashboard";
    }

    // ── MIS REPORTES ───────────────────────────────────────────────────
    @GetMapping("/mis-reportes")
    public String misReportes(@AuthenticationPrincipal Usuario usuario, Model model) {
        model.addAttribute("reportes", reporteService.findByUsuario(usuario.getId()));
        model.addAttribute("categorias", reporteService.findAllCategorias());
        return "user/mis-reportes";
    }

    // ── EDITAR ─────────────────────────────────────────────────────────
    @PostMapping("/editar/{id}")
    public String editar(@PathVariable Integer id,
            @RequestParam String titulo,
            @RequestParam String descripcion,
            @RequestParam Integer categoriaId,
            @RequestParam String ubicacion,
            @RequestParam(required = false) Double latitud,
            @RequestParam(required = false) Double longitud,
            @AuthenticationPrincipal Usuario usuario,
            RedirectAttributes redirectAttributes) {

        Optional<Reporte> opt = reporteService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Reporte no encontrado.");
            return "redirect:/reportes/mis-reportes";
        }

        boolean esAdmin = usuario.isAdmin();
        if (!opt.get().getUsuarioId().equals(usuario.getId()) && !esAdmin) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para editar este reporte.");
            return "redirect:/reportes/mis-reportes";
        }

        reporteService.actualizar(id, titulo, descripcion, categoriaId, ubicacion, latitud, longitud);
        redirectAttributes.addFlashAttribute("exito", "Reporte actualizado correctamente.");
        return "redirect:/reportes/mis-reportes";
    }

    // ── ELIMINAR ───────────────────────────────────────────────────────
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuario,
            RedirectAttributes redirectAttributes) {
        boolean esAdmin = usuario.isAdmin();

        boolean eliminado = esAdmin
                ? reporteService.eliminar(id)
                : reporteService.eliminarSiEsDueno(id, usuario.getId());

        if (eliminado) {
            redirectAttributes.addFlashAttribute("exito", "Reporte eliminado.");
        } else {
            redirectAttributes.addFlashAttribute("error", "No se pudo eliminar el reporte.");
        }

        return esAdmin ? "redirect:/admin" : "redirect:/reportes/mis-reportes";
    }
}
