package com.masseria.service;

import com.masseria.entity.Usuario;
import com.masseria.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
@Transactional
public class UsuarioService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }
    
    public Optional<Usuario> obtenerPorId(Long id) {
        return Optional.ofNullable(id)
                .flatMap(usuarioRepository::findById);
    }
    
    public Optional<Usuario> obtenerPorUsername(String username) {
        return Optional.ofNullable(username)
                .filter(u -> !u.trim().isEmpty())
                .flatMap(usuarioRepository::findByUsername);
    }
    
    public Optional<Usuario> obtenerPorEmail(String email) {
        return Optional.ofNullable(email)
                .filter(e -> !e.trim().isEmpty())
                .flatMap(usuarioRepository::findByEmail);
    }
    
    public Optional<Usuario> obtenerPorDni(String dni) {
        return Optional.ofNullable(dni)
                .filter(d -> !d.trim().isEmpty())
                .flatMap(usuarioRepository::findByDni);
    }
    
    public Usuario guardar(Usuario usuario) {
        Objects.requireNonNull(usuario, "El usuario no puede ser nulo");
        
        // Validaciones básicas
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        
        // Verificar que no exista duplicados
        if (usuario.getId() == null) {
            // Nuevo usuario
            if (usuarioRepository.existsByEmail(usuario.getEmail())) {
                throw new IllegalArgumentException("Ya existe un usuario con el email: " + usuario.getEmail());
            }
            if (usuario.getDni() != null && usuarioRepository.existsByDni(usuario.getDni())) {
                throw new IllegalArgumentException("Ya existe un usuario con el DNI: " + usuario.getDni());
            }
        }
        
        return usuarioRepository.save(usuario);
    }
    
    public void eliminar(Long id) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Usuario no encontrado con ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }
    
    public boolean existeUsername(String username) {
        return Optional.ofNullable(username)
                .filter(u -> !u.trim().isEmpty())
                .map(usuarioRepository::existsByUsername)
                .orElse(false);
    }
    
    public boolean existeEmail(String email) {
        return Optional.ofNullable(email)
                .filter(e -> !e.trim().isEmpty())
                .map(usuarioRepository::existsByEmail)
                .orElse(false);
    }
    
    public boolean existeDni(String dni) {
        return Optional.ofNullable(dni)
                .filter(d -> !d.trim().isEmpty())
                .map(usuarioRepository::existsByDni)
                .orElse(false);
    }
    
    public List<Usuario> obtenerActivos() {
        return usuarioRepository.findByActivoTrue();
    }
    
    public List<Usuario> obtenerPorRol(String rol) {
        return Optional.ofNullable(rol)
                .filter(r -> !r.trim().isEmpty())
                .map(usuarioRepository::findByRol)
                .orElse(List.of());
    }
    
    public Optional<Usuario> login(String email, String password) {
        Objects.requireNonNull(email, "El email no puede ser nulo");
        Objects.requireNonNull(password, "La contraseña no puede ser nula");
        
        return usuarioRepository.findByEmailAndActivoTrue(email)
                .filter(usuario -> usuario.getPassword().equals(password));
    }
    
    public Usuario actualizarUltimoAcceso(Long id) {
        return obtenerPorId(id)
                .map(usuario -> {
                    usuario.setUltimoAcceso(java.time.LocalDateTime.now());
                    return usuarioRepository.save(usuario);
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
    
    public Usuario cambiarRol(Long id, String nuevoRol) {
        return obtenerPorId(id)
                .map(usuario -> {
                    usuario.setRol(nuevoRol);
                    return usuarioRepository.save(usuario);
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
    
    public Usuario activarDesactivar(Long id, boolean activo) {
        return obtenerPorId(id)
                .map(usuario -> {
                    usuario.setActivo(activo);
                    return usuarioRepository.save(usuario);
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
}