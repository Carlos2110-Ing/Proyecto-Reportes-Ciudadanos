package com.utp.reportes_ciudadanos.controller;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    @RequestMapping
    public String handleError(HttpServletRequest request, Model model) {

        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        int status = statusAttr != null
                ? Integer.parseInt(statusAttr.toString())
                : 500;

        model.addAttribute("status", status);

        if (status == 404) {
            model.addAttribute("mensaje", "La página que buscas no existe.");
            return "error/404";
        }

        if (status == 403) {
            model.addAttribute("mensaje", "No tienes permiso para acceder a este recurso.");
            return "error/403";
        }

        if (status == 500) {
            model.addAttribute("mensaje", "Ocurrió un error interno en el servidor.");
            return "error/500";
        }

        model.addAttribute("mensaje", "Ocurrió un error inesperado.");
        return "error/general";
    }
    
    @RequestMapping("/403")
    public String accessDenied(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("mensaje", "No tienes permiso para acceder a este recurso.");
        return "error/403";
    }

    @RequestMapping("/404")
    public String notFound(Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("mensaje", "La página que buscas no existe.");
        return "error/404";
    }

    @RequestMapping("/500")
    public String internalError(Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("mensaje", "Ocurrió un error interno en el servidor.");
        return "error/500";
    }
}
