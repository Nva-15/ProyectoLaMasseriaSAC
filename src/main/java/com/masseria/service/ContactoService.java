package com.masseria.service;

import com.masseria.entity.Contacto;
import com.masseria.repository.ContactoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
@Transactional
public class ContactoService {
    
    @Autowired
    private ContactoRepository contactoRepository;
    
    public List<Contacto> obtenerTodos() {
        return contactoRepository.findAll();
    }
    
    public Optional<Contacto> obtenerPorId(Long id) {
        return Optional.ofNullable(id)
                .flatMap(contactoRepository::findById);
    }
    
    public Contacto guardar(Contacto contacto) {
        Objects.requireNonNull(contacto, "El contacto no puede ser nulo");
        return contactoRepository.save(contacto);
    }
    
    public List<Contacto> obtenerNoLeidos() {
        return contactoRepository.findByLeidoFalse();
    }
    
    public List<Contacto> obtenerPorUsuario(Long usuarioId) {
        return Optional.ofNullable(usuarioId)
                .map(contactoRepository::findByUsuarioId)
                .orElse(List.of());
    }
    
    public void marcarComoLeido(Long id) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        contactoRepository.findById(id).ifPresent(contacto -> {
            contacto.setLeido(true);
            contactoRepository.save(contacto);
        });
    }
    
    public void eliminar(Long id) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        contactoRepository.deleteById(id);
    }
    
    public List<Contacto> obtenerTodosOrdenados() {
        return contactoRepository.findAllByOrderByFechaCreacionDesc();
    }
    
    public List<Contacto> obtenerNoLeidosOrdenados() {
        return contactoRepository.findByLeidoFalseOrderByFechaCreacionDesc();
    }
}