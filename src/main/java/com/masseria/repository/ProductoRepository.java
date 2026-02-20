package com.masseria.repository;

import com.masseria.entity.Categoria;
import com.masseria.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    @Query("SELECT p FROM Producto p WHERE LOWER(p.categoria.nombre) = LOWER(:categoria) AND p.activo = true")
    List<Producto> findByCategoriaAndActivoTrue(@Param("categoria") String categoria);
    
    List<Producto> findByActivoTrue();
    
    List<Producto> findByDestacadoTrueAndActivoTrue();
    
    List<Producto> findByCategoriaAndActivoTrue(Categoria categoria);
    
    List<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
    
    List<Producto> findByPrecioBetweenAndActivoTrue(BigDecimal precioMin, BigDecimal precioMax);
}