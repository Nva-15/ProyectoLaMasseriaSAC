package com.masseria.service;

import com.masseria.entity.Reservacion;
import com.masseria.repository.ReservacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
@Transactional
public class ReservacionService {
    
    @Autowired
    private ReservacionRepository reservacionRepository;
    
    public List<Reservacion> obtenerTodas() {
        return reservacionRepository.findAll();
    }
    
    public Optional<Reservacion> obtenerPorId(Long id) {
        return Optional.ofNullable(id)
                .flatMap(reservacionRepository::findById);
    }
    
    public Reservacion guardar(Reservacion reservacion) {
        Objects.requireNonNull(reservacion, "La reservación no puede ser nula");
        return reservacionRepository.save(reservacion);
    }
    
    public List<Reservacion> obtenerPorUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return List.of(); 
        }
        return reservacionRepository.findByUsuarioId(usuarioId);
    }
    
    public List<Reservacion> obtenerPorFecha(LocalDate fecha) {
        if (fecha == null) {
            return List.of();
        }
        return reservacionRepository.findByFecha(fecha);
    }
    
    public List<Reservacion> obtenerPorUsuarioOrdenadas(Long usuarioId) {
        if (usuarioId == null) {
            return List.of();
        }
        return reservacionRepository.findByUsuarioIdOrderByFechaDescHoraDesc(usuarioId);
    }
    
    public List<Reservacion> obtenerProximasReservaciones() {
        return reservacionRepository.findProximasReservaciones(LocalDate.now());
    }
    
    public boolean verificarDisponibilidad(LocalDate fecha, LocalTime hora) {
        if (fecha == null || hora == null) {
            return false;
        }
        List<Reservacion> reservaciones = reservacionRepository.findByFechaAndHora(fecha, hora);
        return reservaciones.size() < 5; 
    }
    
    public void cancelarReservacion(Long id) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        reservacionRepository.findById(id).ifPresent(reservacion -> {
            reservacion.setEstado("CANCELADA");
            reservacionRepository.save(reservacion);
        });
    }
    
    public void confirmarReservacion(Long id) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        reservacionRepository.findById(id).ifPresent(reservacion -> {
            reservacion.setEstado("CONFIRMADA");
            reservacionRepository.save(reservacion);
        });
    }
    
    public List<Reservacion> obtenerReservacionesDelDia(LocalDate fecha) {
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        return reservacionRepository.findByFecha(fecha);
    }

    public List<Reservacion> obtenerHistorialPorUsuario(Long usuarioId, String periodo,
            LocalDate fechaInicioParam, LocalDate fechaFinParam, String estado) {
        if (usuarioId == null) return List.of();

        LocalDate inicio = null;
        LocalDate fin    = null;

        if (fechaInicioParam != null || fechaFinParam != null) {
            inicio = fechaInicioParam;
            fin    = fechaFinParam;
        } else if (periodo != null) {
            switch (periodo) {
                case "proximas" -> inicio = LocalDate.now();
                case "pasadas"  -> fin    = LocalDate.now().minusDays(1);
                case "hoy"      -> { inicio = LocalDate.now(); fin = LocalDate.now(); }
                default         -> { }   // "todas" — sin restricción de fecha
            }
        } else {
            inicio = LocalDate.now();   // por defecto: próximas
        }

        String filtroEstado = (estado != null && !estado.isEmpty()) ? estado : null;
        return reservacionRepository.findHistorialUsuario(usuarioId, inicio, fin, filtroEstado);
    }
}