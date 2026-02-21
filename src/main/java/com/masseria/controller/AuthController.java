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
import java.util.Optional;

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
    public String perfil(HttpSession session, Model model,
                         @RequestParam(required = false) String tab) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // Recargar desde BD para tener datos frescos
        Optional<Usuario> fresh = usuarioRepository.findById(usuario.getId());
        if (fresh.isPresent()) {
            session.setAttribute("usuario", fresh.get());
            usuario = fresh.get();
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("tab", tab != null ? tab : "info");
        model.addAttribute("activePage", "perfil");
        return "perfil";
    }

    @PostMapping("/perfil/editar")
    public String editarPerfil(HttpSession session,
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String nuevaPassword,
            @RequestParam(required = false) String confirmarPassword,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()
                && !nuevaPassword.equals(confirmarPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/perfil?editar=true";
        }

        try {
            Usuario u = usuarioRepository.findById(usuario.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Email único si cambió
            if (email != null && !email.trim().isEmpty()
                    && !email.trim().equalsIgnoreCase(u.getEmail())) {
                if (usuarioRepository.existsByEmail(email.trim())) {
                    redirectAttributes.addFlashAttribute("error", "Ese email ya está registrado por otro usuario");
                    return "redirect:/perfil?editar=true";
                }
                u.setEmail(email.trim());
                u.setUsername(email.trim());
            }

            // DNI único si cambió
            if (dni != null && !dni.trim().isEmpty()
                    && !dni.trim().equals(u.getDni())) {
                if (usuarioRepository.existsByDni(dni.trim())) {
                    redirectAttributes.addFlashAttribute("error", "Ese DNI ya está registrado por otro usuario");
                    return "redirect:/perfil?editar=true";
                }
                u.setDni(dni.trim());
            } else if (dni != null && dni.trim().isEmpty()) {
                u.setDni(null);
            }

            if (nombres   != null) u.setNombres(nombres.trim().isEmpty()   ? null : nombres.trim());
            if (apellidos != null) u.setApellidos(apellidos.trim().isEmpty() ? null : apellidos.trim());
            if (telefono  != null) u.setTelefono(telefono.trim().isEmpty()  ? null : telefono.trim());
            if (direccion != null) u.setDireccion(direccion.trim().isEmpty() ? null : direccion.trim());

            if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
                u.setPassword(passwordEncoder.encode(nuevaPassword.trim()));
            }

            Usuario actualizado = usuarioRepository.save(u);
            session.setAttribute("usuario", actualizado);
            session.setAttribute("usuarioNombre",
                    actualizado.getNombres() + " " + actualizado.getApellidos());

            redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/perfil?editar=true";
        }
        return "redirect:/perfil";
    }
}