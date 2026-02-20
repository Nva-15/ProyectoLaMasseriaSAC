package com.masseria.service;

import com.masseria.entity.Producto;
import com.masseria.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoService {
    
    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> obtenerTodos() {
        return productoRepository.findByActivoTrue();
    }

    public List<Producto> obtenerTodosAdmin() {
        return productoRepository.findAll();
    }

    public List<Producto> obtenerPorCategoria(String categoria) {
        return productoRepository.findByCategoriaAndActivoTrue(categoria);
    }

    public Optional<Producto> obtenerPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return productoRepository.findById(id);
    }

    public Producto guardar(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo");
        }
        return productoRepository.save(producto);
    }

    public void eliminar(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser nulo");
        }
        productoRepository.deleteById(id);
    }

    public List<Producto> buscar(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return obtenerTodos();
        }
        return productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(termino);
    }

    public List<Producto> obtenerDestacados() {
        return productoRepository.findByDestacadoTrueAndActivoTrue();
    }
    
    public List<Producto> obtenerPorRangoPrecio(BigDecimal min, BigDecimal max) {
        return productoRepository.findByPrecioBetweenAndActivoTrue(min, max);
    }
    
    public Producto actualizarStock(Long id, Integer cantidad) {
        Optional<Producto> productoOpt = obtenerPorId(id);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            producto.setStock(producto.getStock() + cantidad);
            return productoRepository.save(producto);
        }
        throw new RuntimeException("Producto no encontrado con id: " + id);
    }
}