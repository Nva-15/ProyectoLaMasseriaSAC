package com.masseria.service;

import com.masseria.entity.Pedido;
import com.masseria.entity.Usuario;
import com.masseria.repository.PedidoRepository;
import com.masseria.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.Objects;

@Service
@Transactional
@SuppressWarnings("null") 
public class PedidoService {
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    // Constantes para los estados válidos
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_EN_PROCESO = "EN_PROCESO";
    public static final String ESTADO_ENVIADO = "ENVIADO";
    public static final String ESTADO_ENTREGADO = "ENTREGADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    
    // Constantes para tipos de entrega
    public static final String ENTREGA_RECOGER = "RECOGER";
    public static final String ENTREGA_DOMICILIO = "DOMICILIO";
    
    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }
    
    public Optional<Pedido> obtenerPorId(Long id) {
        return Optional.ofNullable(id)
                .flatMap(pedidoRepository::findById);
    }
    
    public List<Pedido> obtenerPorUsuario(Long usuarioId) {
        return Optional.ofNullable(usuarioId)
                .map(pedidoRepository::findByUsuarioId)
                .orElse(Collections.emptyList());
    }
    
    public List<Pedido> obtenerPorUsuarioYEstado(Long usuarioId, String estado) {
        if (usuarioId == null || estado == null) {
            return Collections.emptyList();
        }
        return pedidoRepository.findByUsuarioIdAndEstado(usuarioId, estado);
    }
    
    public List<Pedido> obtenerPorUsuarioRecientes(Long usuarioId) {
        return Optional.ofNullable(usuarioId)
                .map(pedidoRepository::findByUsuarioIdOrderByFechaPedidoDesc)
                .orElse(Collections.emptyList());
    }
    
    public List<Pedido> obtenerPorEstado(String estado) {
        return Optional.ofNullable(estado)
                .filter(e -> !e.trim().isEmpty())
                .map(pedidoRepository::findByEstado)
                .orElse(Collections.emptyList());
    }
    
    public List<Pedido> obtenerPedidosPendientes() {
        return pedidoRepository.findByEstado(ESTADO_PENDIENTE);
    }
    
    public List<Pedido> obtenerPedidosEnProceso() {
        return pedidoRepository.findByEstado(ESTADO_EN_PROCESO);
    }
    
    public List<Pedido> obtenerPedidosEntregados() {
        return pedidoRepository.findByEstado(ESTADO_ENTREGADO);
    }
    
    public List<Pedido> obtenerPedidosCancelados() {
        return pedidoRepository.findByEstado(ESTADO_CANCELADO);
    }
    
   public List<Pedido> obtenerPorFecha(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return Collections.emptyList();
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha inicio debe ser anterior a la fecha fin");
        }
        return pedidoRepository.findByFechaPedidoBetween(fechaInicio, fechaFin);
    }
    
    public List<Pedido> obtenerPedidosDelDia() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return pedidoRepository.findByFechaPedidoBetween(inicio, fin);
    }
    
    public List<Pedido> obtenerPedidosDeLaSemana() {
        LocalDateTime inicio = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime fin = LocalDateTime.now();
        return pedidoRepository.findByFechaPedidoBetween(inicio, fin);
    }
    
    public List<Pedido> obtenerPorTipoEntrega(String tipoEntrega) {
        if (tipoEntrega == null || tipoEntrega.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return pedidoRepository.findByTipoEntrega(tipoEntrega);
    }
    
    public List<Pedido> obtenerPedidosRecoger() {
        return pedidoRepository.findByTipoEntrega(ENTREGA_RECOGER);
    }
    
    public List<Pedido> obtenerPedidosDomicilio() {
        return pedidoRepository.findByTipoEntrega(ENTREGA_DOMICILIO);
    }
    
    public List<Pedido> obtenerPorMetodoPago(String metodoPago) {
        if (metodoPago == null || metodoPago.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return pedidoRepository.findByMetodoPago(metodoPago);
    }
    
    public Pedido guardar(Pedido pedido) {
        Objects.requireNonNull(pedido, "El pedido no puede ser nulo");
        
        Usuario usuario = pedido.getUsuario();
        if (usuario == null) {
            throw new IllegalArgumentException("El pedido debe tener un usuario asociado");
        }
        
        Long usuarioId = usuario.getId();
        if (usuarioId == null) {
            throw new IllegalArgumentException("El usuario no tiene un ID válido");
        }
        
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new IllegalArgumentException("El usuario especificado no existe");
        }
        
        // Validar campos obligatorios
        if (pedido.getClienteNombre() == null || pedido.getClienteNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        
        if (pedido.getClienteTelefono() == null || pedido.getClienteTelefono().trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono del cliente es obligatorio");
        }
        
        if (pedido.getTipoEntrega() == null || pedido.getTipoEntrega().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de entrega es obligatorio");
        }
        
        if (pedido.getMetodoPago() == null || pedido.getMetodoPago().trim().isEmpty()) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        
        // Validar dirección si es envío a domicilio
        if (ENTREGA_DOMICILIO.equals(pedido.getTipoEntrega()) && 
            (pedido.getDireccionEntrega() == null || pedido.getDireccionEntrega().trim().isEmpty())) {
            throw new IllegalArgumentException("La dirección es obligatoria para envío a domicilio");
        }
        
        if (!esEstadoValido(pedido.getEstado())) {
            pedido.setEstado(ESTADO_PENDIENTE);
        }
        
        return pedidoRepository.save(pedido);
    }
    
    public Pedido guardarConUsuarioId(Pedido pedido, Long usuarioId) {
        Objects.requireNonNull(pedido, "El pedido no puede ser nulo");
        Objects.requireNonNull(usuarioId, "El ID del usuario no puede ser nulo");
        
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
        
        pedido.setUsuario(usuario);
        
        if (pedido.getClienteNombre() == null) {
            pedido.setClienteNombre(usuario.getNombreCompleto());
        }
        if (pedido.getClienteEmail() == null) {
            pedido.setClienteEmail(usuario.getEmail());
        }
        if (pedido.getClienteTelefono() == null) {
            pedido.setClienteTelefono(usuario.getTelefono());
        }
        if (pedido.getDireccionEntrega() == null) {
            pedido.setDireccionEntrega(usuario.getDireccion());
        }
        
        if (!esEstadoValido(pedido.getEstado())) {
            pedido.setEstado(ESTADO_PENDIENTE);
        }
        
        return pedidoRepository.save(pedido);
    }
    
    public Pedido actualizarEstado(Long id, String nuevoEstado) {
        Objects.requireNonNull(id, "El id no puede ser nulo");
        Objects.requireNonNull(nuevoEstado, "El estado no puede ser nulo");
        
        if (nuevoEstado.trim().isEmpty()) {
            throw new IllegalArgumentException("El estado no puede estar vacío");
        }
        
        if (!esEstadoValido(nuevoEstado)) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado + 
                ". Los estados permitidos son: " + obtenerEstadosPermitidos());
        }
        
        return pedidoRepository.findById(id)
            .map(pedido -> {
                pedido.setEstado(nuevoEstado);
                return pedidoRepository.save(pedido);
            })
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
    }
    
    public Pedido actualizarTipoEntrega(Long id, String tipoEntrega, String direccion) {
        return pedidoRepository.findById(id)
            .map(pedido -> {
                pedido.setTipoEntrega(tipoEntrega);
                if (ENTREGA_DOMICILIO.equals(tipoEntrega) && direccion != null) {
                    pedido.setDireccionEntrega(direccion);
                }
                return pedidoRepository.save(pedido);
            })
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
    }
    
    public void eliminar(Long id) {
        Objects.requireNonNull(id, "El id no puede ser nulo");
        
        if (!pedidoRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: Pedido no encontrado con id: " + id);
        }
        pedidoRepository.deleteById(id);
    }
    
    public void cancelarPedido(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        pedidoRepository.findById(id).ifPresentOrElse(
            pedido -> {
                pedido.setEstado(ESTADO_CANCELADO);
                pedidoRepository.save(pedido);
            },
            () -> { throw new RuntimeException("Pedido no encontrado con id: " + id); }
        );
    }
    
    private boolean esEstadoValido(String estado) {
        return estado != null && (
            estado.equals(ESTADO_PENDIENTE) ||
            estado.equals(ESTADO_EN_PROCESO) ||
            estado.equals(ESTADO_ENVIADO) ||
            estado.equals(ESTADO_ENTREGADO) ||
            estado.equals(ESTADO_CANCELADO)
        );
    }
    
    private String obtenerEstadosPermitidos() {
        return String.join(", ", 
            ESTADO_PENDIENTE, 
            ESTADO_EN_PROCESO, 
            ESTADO_ENVIADO, 
            ESTADO_ENTREGADO, 
            ESTADO_CANCELADO);
    }
    
    public boolean puedeCancelarPedido(Long id) {
        if (id == null) return false;
        return obtenerPorId(id)
            .map(pedido -> 
                pedido.getEstado().equals(ESTADO_PENDIENTE) || 
                pedido.getEstado().equals(ESTADO_EN_PROCESO))
            .orElse(false);
    }
    
    public long contarPedidosPendientes() {
        return pedidoRepository.countByEstado(ESTADO_PENDIENTE);
    }
    
    public long contarPedidosHoy() {
        return obtenerPedidosDelDia().size();
    }
    
    public BigDecimal calcularTotalVentasHoy() {
        return obtenerPedidosDelDia().stream()
            .map(Pedido::getTotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public List<Pedido> buscarPedidos(String estado, String tipoEntrega, LocalDate fecha) {
        return pedidoRepository.findAll().stream()
            .filter(p -> estado == null || estado.isEmpty() || estado.equals(p.getEstado()))
            .filter(p -> tipoEntrega == null || tipoEntrega.isEmpty() || tipoEntrega.equals(p.getTipoEntrega()))
            .filter(p -> fecha == null || (p.getFechaPedido() != null && p.getFechaPedido().toLocalDate().equals(fecha)))
            .toList();
    }
}