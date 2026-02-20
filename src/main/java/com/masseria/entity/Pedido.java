package com.masseria.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "fecha_pedido", nullable = false)
    private LocalDateTime fechaPedido;
    
    @Column(nullable = false)
    private String estado; // PENDIENTE, EN_PROCESO, ENVIADO, ENTREGADO, CANCELADO
    
    @Column(precision = 10, scale = 2)
    private BigDecimal total;
    
    // RELACIÓN CON USUARIO
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;
    
    @Column(name = "cliente_nombre", nullable = false, length = 200)
    private String clienteNombre;
    
    @Column(name = "cliente_telefono", nullable = false, length = 20)
    private String clienteTelefono;
    
    @Column(name = "cliente_email", length = 100)
    private String clienteEmail;
    
    @Column(name = "tipo_entrega", nullable = false, length = 20)
    private String tipoEntrega; // RECOGER, DOMICILIO
    
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago; // EFECTIVO, TARJETA, YAPE, PLIN
    
    @Column(name = "direccion_entrega", length = 255)
    private String direccionEntrega;
    
    @Column(length = 500)
    private String notas;
    
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<DetallePedido> detalles = new ArrayList<>();
    
    public Pedido(Usuario usuario) {
        this.fechaPedido = LocalDateTime.now();
        this.estado = "PENDIENTE";
        this.total = BigDecimal.ZERO;
        this.usuario = usuario;
        this.detalles = new ArrayList<>();
    }
    
    // Constructor completo para pedidos
    public Pedido(Usuario usuario, String clienteNombre, String clienteTelefono, 
                  String clienteEmail, String tipoEntrega, String metodoPago,
                  String direccionEntrega, String notas) {
        this.fechaPedido = LocalDateTime.now();
        this.estado = "PENDIENTE";
        this.total = BigDecimal.ZERO;
        this.usuario = usuario;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.clienteEmail = clienteEmail;
        this.tipoEntrega = tipoEntrega;
        this.metodoPago = metodoPago;
        this.direccionEntrega = direccionEntrega;
        this.notas = notas;
        this.detalles = new ArrayList<>();
    }
    
    public Long getUsuarioId() {
        return usuario != null ? usuario.getId() : null;
    }
    
    public void addDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
        calcularTotal();
    }
    
    public void removeDetalle(DetallePedido detalle) {
        detalles.remove(detalle);
        detalle.setPedido(null);
        calcularTotal();
    }
    
    private void calcularTotal() {
        this.total = detalles.stream()
            .map(DetallePedido::getSubtotal)
            .filter(subtotal -> subtotal != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public static Pedido crearPedido(Usuario usuario) {
        return Pedido.builder()
            .fechaPedido(LocalDateTime.now())
            .estado("PENDIENTE")
            .total(BigDecimal.ZERO)
            .usuario(usuario)
            .detalles(new ArrayList<>())
            .build();
    }
    
    public boolean esEntregaDomicilio() {
        return "DOMICILIO".equals(this.tipoEntrega);
    }
    
    public String getResumen() {
        return String.format("Pedido #%d - %s - Total: S/ %.2f", 
            id, estado, total != null ? total.doubleValue() : 0);
    }
}