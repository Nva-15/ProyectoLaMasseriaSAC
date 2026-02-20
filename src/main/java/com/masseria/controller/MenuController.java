package com.masseria.controller;

import com.masseria.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private ProductoService productoService;

    @GetMapping("/desayunos")
    public String desayunos(Model model) {
        model.addAttribute("productos", productoService.obtenerPorCategoria("Desayuno"));
        model.addAttribute("titulo", "Desayunos");
        model.addAttribute("activePage", "desayunos");
        return "menu/desayunos";
    }

    @GetMapping("/bebidas-calientes")
    public String bebidasCalientes(Model model) {
        model.addAttribute("productos", productoService.obtenerPorCategoria("Bebida Caliente"));
        model.addAttribute("titulo", "Bebidas Calientes");
        model.addAttribute("activePage", "bebidas-calientes");
        return "menu/bebidas-calientes";
    }

    @GetMapping("/bebidas-frias")
    public String bebidasFrias(Model model) {
        model.addAttribute("productos", productoService.obtenerPorCategoria("Bebida Fría"));
        model.addAttribute("titulo", "Bebidas Frías");
        model.addAttribute("activePage", "bebidas-frias");
        return "menu/bebidas-frias";
    }

    @GetMapping("/postres")
    public String postres(Model model) {
        model.addAttribute("productos", productoService.obtenerPorCategoria("Postre"));
        model.addAttribute("titulo", "Postres");
        model.addAttribute("activePage", "postres");
        return "menu/postres";
    }
}