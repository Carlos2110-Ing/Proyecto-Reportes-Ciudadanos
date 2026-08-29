/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.safeperu.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author Usuario
 */
@Controller
public class AcercaDeController {
    @GetMapping("/acerca-de")
    public String acercaDe(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        return "user/acerca-de";
    }
}
