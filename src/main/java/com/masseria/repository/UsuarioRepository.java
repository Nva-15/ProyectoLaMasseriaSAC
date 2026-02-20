package com.masseria.repository;

import com.masseria.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByUsername(String username);
    
    Optional<Usuario> findByEmail(String email);
    
    Optional<Usuario> findByDni(String dni);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByDni(String dni);
    
    List<Usuario> findByActivoTrue();
    
    List<Usuario> findByRol(String rol);
    
    Optional<Usuario> findByEmailAndActivoTrue(String email);
}