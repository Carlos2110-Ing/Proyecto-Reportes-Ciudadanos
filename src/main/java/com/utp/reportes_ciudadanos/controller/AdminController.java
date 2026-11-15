/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.reportes_ciudadanos.controller;

import com.utp.reportes_ciudadanos.model.Permiso;
import com.utp.reportes_ciudadanos.model.Reporte;
import com.utp.reportes_ciudadanos.model.Rol;
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.ReporteService;
import com.utp.reportes_ciudadanos.service.RolService;
import com.utp.reportes_ciudadanos.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
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
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioService usuarioService;
    private final ReporteService reporteService;
    private final RolService rolService;

    @Autowired
    public AdminController(UsuarioService usuarioService,
            ReporteService reporteService,
            RolService rolService) {
        this.usuarioService = usuarioService;
        this.reporteService = reporteService;
        this.rolService = rolService;
    }

    // ── DASHBOARD ──────────────────────────────────────────────────────
    @GetMapping({ "", "/" })
    public String adminDashboard(Model model) {
        cargarDatosAdmin(model);

        if (!model.containsAttribute("rol")) {
            model.addAttribute("rol", new Rol());
        }

        if (!model.containsAttribute("permiso")) {
            model.addAttribute("permiso", new Permiso());
        }

        return "admin/admin";
    }

    // ── VERIFICACIÓN DE USUARIOS ───────────────────────────────────────
    @PostMapping("/verificar/{usuarioId}")
    public String verificar(@PathVariable Integer usuarioId,
            @AuthenticationPrincipal Usuario admin,
            RedirectAttributes ra) {
        usuarioService.verificarUsuario(usuarioId, admin.getId());
        ra.addFlashAttribute("exito", "Usuario verificado.");
        return "redirect:/admin";
    }

    @PostMapping("/revocar/{usuarioId}")
    public String revocar(@PathVariable Integer usuarioId, RedirectAttributes ra) {
        usuarioService.revocarVerificacion(usuarioId);
        ra.addFlashAttribute("exito", "Verificación revocada.");
        return "redirect:/admin";
    }

    // ── CAMBIAR ROL DE USUARIO ─────────────────────────────────────────
    @PostMapping("/usuarios/rol/{usuarioId}")
    public String cambiarRolUsuario(@PathVariable Integer usuarioId,
            @RequestParam String nuevoRol,
            RedirectAttributes ra) {
        if (!rolService.existeRol(nuevoRol)) {
            ra.addFlashAttribute("error", "El rol '" + nuevoRol + "' no existe.");
            return "redirect:/admin";
        }
        usuarioService.cambiarRol(usuarioId, nuevoRol);
        ra.addFlashAttribute("exito", "Rol actualizado a " + nuevoRol + ".");
        return "redirect:/admin";
    }

    // ── ESTADO DE REPORTES ─────────────────────────────────────────────
    @PostMapping("/reportes/estado/{id}")
    public String cambiarEstado(@PathVariable Integer id,
            @RequestParam String estado,
            RedirectAttributes ra) {
        try {
            reporteService.cambiarEstado(id, Reporte.Estado.valueOf(estado));
            ra.addFlashAttribute("exito", "Estado actualizado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "Estado inválido.");
        }
        return "redirect:/admin";
    }

    // ── GESTIÓN DE ROLES ───────────────────────────────────────────────
    @PostMapping("/roles/crear")
    public String crearRol(@Valid @ModelAttribute("rol") Rol rol,
            BindingResult br,
            Model model,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            cargarDatosAdmin(model);
            model.addAttribute("permiso", new Permiso());
            return "admin/admin";
        }

        String nombre = rol.getNombre().trim().toUpperCase();

        if (rolService.existeRol(nombre)) {
            br.rejectValue("nombre", "duplicado", "El rol '" + nombre + "' ya existe.");
            cargarDatosAdmin(model);
            model.addAttribute("permiso", new Permiso());
            return "admin/admin";
        }

        rolService.crearRol(nombre, rol.getDescripcion().trim());

        ra.addFlashAttribute("exito", "Rol '" + nombre + "' creado.");
        return "redirect:/admin#roles";
    }

    @PostMapping("/roles/eliminar/{id}")
    public String eliminarRol(@PathVariable Integer id, RedirectAttributes ra) {
        rolService.eliminarRol(id);
        ra.addFlashAttribute("exito", "Rol eliminado.");
        return "redirect:/admin#roles";
    }

    @PostMapping("/roles/permisos/{rolId}")
    public String asignarPermisos(@PathVariable Integer rolId,
            @RequestParam(required = false) List<Integer> permisoIds,
            RedirectAttributes ra) {
        rolService.asignarPermisos(rolId, permisoIds != null ? permisoIds : List.of());
        ra.addFlashAttribute("exito", "Permisos actualizados.");
        return "redirect:/admin#roles";
    }

    // ── GESTIÓN DE PERMISOS ────────────────────────────────────────────
    @PostMapping("/permisos/crear")
    public String crearPermiso(@Valid @ModelAttribute("permiso") Permiso permiso,
            BindingResult br,
            Model model,
            RedirectAttributes ra) {
        if (br.hasErrors()) {
            cargarDatosAdmin(model);
            model.addAttribute("rol", new Rol());
            return "admin/admin";
        }

        String urlPattern = permiso.getUrlPattern().trim();
        String httpMethod = permiso.getHttpMethod() == null
                ? "*"
                : permiso.getHttpMethod().trim().toUpperCase();

        if (rolService.existePermiso(urlPattern, httpMethod)) {
            br.rejectValue("urlPattern", "duplicado",
                    "Ya existe un permiso para la ruta '" + urlPattern + "' con método " + httpMethod + ".");

            cargarDatosAdmin(model);
            model.addAttribute("rol", new Rol());
            return "admin/admin";
        }

        rolService.crearPermiso(
                permiso.getUrlPattern().trim(),
                permiso.getDescripcion().trim(),
                permiso.getHttpMethod());

        ra.addFlashAttribute("exito", "Permiso creado para: " + permiso.getUrlPattern());
        return "redirect:/admin#roles";

    }

    @PostMapping("/permisos/eliminar/{id}")
    public String eliminarPermiso(@PathVariable Integer id, RedirectAttributes ra) {
        rolService.eliminarPermiso(id);
        ra.addFlashAttribute("exito", "Permiso eliminado.");
        return "redirect:/admin#roles";
    }

    private void cargarDatosAdmin(Model model) {
        var usuarios = usuarioService.findAll().stream()
                .filter(u -> !u.isAdmin())
                .collect(Collectors.toList());

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("verificaciones", usuarioService.findAllVerificaciones());
        model.addAttribute("totalUsuarios", usuarios.size());
        model.addAttribute("totalVerificados",
                usuarios.stream().filter(u -> usuarioService.isVerificado(u.getId())).count());
        model.addAttribute("totalReportes", reporteService.countTotal());
        model.addAttribute("pendientes", reporteService.countPendiente());
        model.addAttribute("ultimosReportes", reporteService.findAll());
        model.addAttribute("roles", rolService.findAllRoles());
        model.addAttribute("permisos", rolService.findAllPermisos());
    }
}
