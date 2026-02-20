package com.masseria.repository;

import com.masseria.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    @Query("SELECT p FROM Pedido p WHERE p.usuario.id = :usuarioId")
    List<Pedido> findByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT p FROM Pedido p WHERE p.usuario.id = :usuarioId AND p.estado = :estado")
    List<Pedido> findByUsuarioIdAndEstado(@Param("usuarioId") Long usuarioId, @Param("estado") String estado);
    
    @Query("SELECT p FROM Pedido p WHERE p.usuario.id = :usuarioId ORDER BY p.fechaPedido DESC")
    List<Pedido> findByUsuarioIdOrderByFechaPedidoDesc(@Param("usuarioId") Long usuarioId);
    
    List<Pedido> findByEstado(String estado);
    
    @Query("SELECT p FROM Pedido p WHERE p.estado IN :estados")
    List<Pedido> findByEstados(@Param("estados") List<String> estados);
    
    List<Pedido> findByFechaPedidoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
    
    @Query("SELECT p FROM Pedido p WHERE DATE(p.fechaPedido) = :fecha")
    List<Pedido> findByFechaPedido(@Param("fecha") LocalDate fecha);
    
    @Query("SELECT p FROM Pedido p WHERE p.fechaPedido >= :fecha ORDER BY p.fechaPedido DESC")
    List<Pedido> findPedidosRecientes(@Param("fecha") LocalDateTime fecha);
    
    List<Pedido> findByTipoEntrega(String tipoEntrega);
    
    @Query("SELECT p FROM Pedido p WHERE p.tipoEntrega = :tipoEntrega AND p.estado = :estado")
    List<Pedido> findByTipoEntregaAndEstado(@Param("tipoEntrega") String tipoEntrega, @Param("estado") String estado);
    
    List<Pedido> findByMetodoPago(String metodoPago);
    
    // ===== CONSULTAS ESTADÍSTICAS =====
    
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estado = :estado")
    long countByEstado(@Param("estado") String estado);
    
    @Query("SELECT SUM(p.total) FROM Pedido p WHERE DATE(p.fechaPedido) = :fecha")
    BigDecimal sumTotalByFecha(@Param("fecha") LocalDate fecha);
    
    @Query("SELECT p.estado, COUNT(p) FROM Pedido p GROUP BY p.estado")
    List<Object[]> countPedidosByEstado();
    
    // ===== BÚSQUEDAS AVANZADAS =====
    
    @Query("SELECT p FROM Pedido p WHERE " +
           "(:usuarioId IS NULL OR p.usuario.id = :usuarioId) AND " +
           "(:estado IS NULL OR p.estado = :estado) AND " +
           "(:tipoEntrega IS NULL OR p.tipoEntrega = :tipoEntrega)")
    List<Pedido> buscarPedidos(
            @Param("usuarioId") Long usuarioId,
            @Param("estado") String estado,
            @Param("tipoEntrega") String tipoEntrega);
    
    @Query("SELECT p FROM Pedido p WHERE p.clienteTelefono = :telefono")
    List<Pedido> findByClienteTelefono(@Param("telefono") String telefono);
    
    @Query("SELECT p FROM Pedido p WHERE p.clienteEmail = :email")
    List<Pedido> findByClienteEmail(@Param("email") String email);

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN p.detalles d WHERE " +
           "p.usuario.id = :usuarioId AND " +
           "(:fechaInicio IS NULL OR p.fechaPedido >= :fechaInicio) AND " +
           "(:fechaFin IS NULL OR p.fechaPedido <= :fechaFin) AND " +
           "(:nombreProducto IS NULL OR LOWER(d.producto.nombre) LIKE LOWER(CONCAT('%', :nombreProducto, '%'))) " +
           "ORDER BY p.fechaPedido DESC")
    List<Pedido> findHistorialUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("nombreProducto") String nombreProducto);
}