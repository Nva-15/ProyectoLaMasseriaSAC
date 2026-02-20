package com.masseria.service;

import com.masseria.entity.Categoria;
import com.masseria.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@Transactional
@SuppressWarnings("null") 
public class CategoriaService {
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    public List<Categoria> obtenerTodas() {
        return categoriaRepository.findAll();
    }
    
    public Optional<Categoria> obtenerPorId(Long id) {
        return id != null ? categoriaRepository.findById(id) : Optional.empty();
    }
    
    public Optional<Categoria> obtenerPorNombre(String nombre) {
        return (nombre != null && !nombre.trim().isEmpty()) 
            ? categoriaRepository.findByNombre(nombre.trim()) 
            : Optional.empty();
    }
    
    public Categoria guardar(Categoria categoria) {
        validarCategoria(categoria);
        verificarNombreUnico(categoria);
        categoria.setNombre(categoria.getNombre().trim());
        return categoriaRepository.save(categoria);
    }
    
    public void eliminar(Long id) {
        validarId(id);
        Categoria categoria = buscarCategoriaPorId(id);
        verificarSinProductosAsociados(categoria);
        categoriaRepository.deleteById(id);
    }
    
    public Categoria actualizar(Long id, Categoria categoriaActualizada) {
        validarId(id);
        validarCategoriaActualizada(categoriaActualizada);
        
        Categoria categoriaExistente = buscarCategoriaPorId(id);
        actualizarCampos(categoriaExistente, categoriaActualizada);
        
        return categoriaRepository.save(categoriaExistente);
    }
    
    public List<Categoria> buscarPorNombre(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String terminoLower = termino.toLowerCase().trim();
        
        return categoriaRepository.findAll().stream()
            .filter(c -> c.getNombre() != null)
            .filter(c -> c.getNombre().toLowerCase().contains(terminoLower))
            .collect(Collectors.toList());
    }
    
    public long contarCategorias() {
        return categoriaRepository.count();
    }
    
    public boolean existeCategoria(Long id) {
        return id != null && categoriaRepository.existsById(id);
    }
    
    public boolean existeCategoriaPorNombre(String nombre) {
        return nombre != null && !nombre.trim().isEmpty()
            ? categoriaRepository.findByNombreIgnoreCase(nombre.trim()).isPresent()
            : false;
    }
    
    public List<Categoria> obtenerCategoriasConProductos() {
        return categoriaRepository.findAll().stream()
            .filter(c -> c.getProductos() != null && !c.getProductos().isEmpty())
            .collect(Collectors.toList());
    }
    
    public List<Categoria> obtenerCategoriasSinProductos() {
        return categoriaRepository.findAll().stream()
            .filter(c -> c.getProductos() == null || c.getProductos().isEmpty())
            .collect(Collectors.toList());
    }
    
    private void validarCategoria(Categoria categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría no puede ser nula");
        }
        if (categoria.getNombre() == null || categoria.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
    }
    
    private void validarCategoriaActualizada(Categoria categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría actualizada no puede ser nula");
        }
    }
    
    private void validarId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
    }
    
    private Categoria buscarCategoriaPorId(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));
    }
    
    private void verificarNombreUnico(Categoria categoria) {
        Optional<Categoria> existente = categoriaRepository.findByNombreIgnoreCase(
            categoria.getNombre().trim()
        );

        if (existente.isPresent() &&
            (categoria.getId() == null || !existente.get().getId().equals(categoria.getId()))) {
            throw new IllegalArgumentException(
                "Ya existe una categoría con el nombre: " + categoria.getNombre()
            );
        }
    }
    
    private void verificarSinProductosAsociados(Categoria categoria) {
        if (categoria.getProductos() != null && !categoria.getProductos().isEmpty()) {
            throw new RuntimeException(
                "No se puede eliminar la categoría porque tiene " + 
                categoria.getProductos().size() + " productos asociados"
            );
        }
    }
    
    private void actualizarCampos(Categoria destino, Categoria origen) {
        if (origen.getNombre() != null && !origen.getNombre().trim().isEmpty()) {
            String nuevoNombre = origen.getNombre().trim();
            verificarNombreUnicoParaActualizacion(nuevoNombre, destino.getId());
            destino.setNombre(nuevoNombre);
        }
        
        if (origen.getDescripcion() != null) {
            destino.setDescripcion(origen.getDescripcion().trim());
        }
    }
    
    private void verificarNombreUnicoParaActualizacion(String nuevoNombre, Long idActual) {
        Optional<Categoria> existente = categoriaRepository.findByNombreIgnoreCase(nuevoNombre);

        if (existente.isPresent() && !existente.get().getId().equals(idActual)) {
            throw new IllegalArgumentException(
                "Ya existe otra categoría con el nombre: " + nuevoNombre
            );
        }
    }
}