package com.masseria.repository;

import com.masseria.entity.Reservacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;  
import java.util.List;

@Repository
public interface ReservacionRepository extends JpaRepository<Reservacion, Long> {
    
    List<Reservacion> findByUsuarioId(Long usuarioId);
    
    List<Reservacion> findByFecha(LocalDate fecha);
    
    @Query("SELECT r FROM Reservacion r WHERE r.fecha = :fecha AND r.hora = :hora")
    List<Reservacion> findByFechaAndHora(@Param("fecha") LocalDate fecha, @Param("hora") LocalTime hora);
    
    List<Reservacion> findByEstado(String estado);
    
    List<Reservacion> findByUsuarioIdOrderByFechaDescHoraDesc(Long usuarioId);
    
    @Query("SELECT r FROM Reservacion r WHERE r.fecha >= :fecha ORDER BY r.fecha ASC, r.hora ASC")
    List<Reservacion> findProximasReservaciones(@Param("fecha") LocalDate fecha);

    @Query("SELECT r FROM Reservacion r WHERE r.usuario.id = :usuarioId " +
           "AND (:fechaInicio IS NULL OR r.fecha >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR r.fecha <= :fechaFin) " +
           "AND (:estado IS NULL OR r.estado = :estado) " +
           "ORDER BY r.fecha DESC, r.hora DESC")
    List<Reservacion> findHistorialUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("estado") String estado);
}