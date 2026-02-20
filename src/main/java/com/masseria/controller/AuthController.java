package com.masseria.controller;

import com.masseria.entity.Usuario;
import com.masseria.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Controller
public class AuthController {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;  // <-- INYECTAR PasswordEncoder
    
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("activePage", "login");
        return "login";
    }
    
    @PostMapping("/login")
    public String login(@RequestParam String email, 
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(u -> u.getActivo())
                .orElse(null);
        
        // VERIFICAR CON PASSWORD ENCRIPTADO
        if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
            
            session.setAttribute("usuario", usuario);
            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("usuarioNombre", usuario.getNombres() + " " + usuario.getApellidos());
            
            redirectAttributes.addFlashAttribute("mensaje", "¡Bienvenido " + usuario.getNombres() + "!");
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", "Credenciales incorrectas");
            return "redirect:/login?error";
        }
    }
    
    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("activePage", "registro");
        return "registro";
    }
    
    @PostMapping("/registro")
    public String registro(@ModelAttribute Usuario usuario,
                           @RequestParam String confirmPassword,
                           RedirectAttributes redirectAttributes) {
        
        if (!usuario.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/registro?error=password";
        }
        
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "El email ya está registrado");
            return "redirect:/registro?error=email";
        }
        
        if (usuarioRepository.existsByDni(usuario.getDni())) {
            redirectAttributes.addFlashAttribute("error", "El DNI ya está registrado");
            return "redirect:/registro?error=dni";
        }
        
        // ASIGNAR USERNAME (usando el email)
        usuario.setUsername(usuario.getEmail());
        
        // ===== PASO IMPORTANTE: CODIFICAR LA CONTRASEÑA =====
        String passwordCodificado = passwordEncoder.encode(usuario.getPassword());
        usuario.setPassword(passwordCodificado);
        
        usuario.setActivo(true);
        usuario.setRol("CLIENTE");
        usuario.setFechaRegistro(LocalDateTime.now());
        
        usuarioRepository.save(usuario);
        
        redirectAttributes.addFlashAttribute("mensaje", "Registro exitoso. ¡Ya puedes iniciar sesión!");
        return "redirect:/login?registro=exitoso";
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
    
    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("activePage", "perfil");
        return "perfil";
    }
}