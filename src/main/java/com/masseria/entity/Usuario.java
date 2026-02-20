package com.masseria.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "dni", unique = true, length = 8)
    private String dni;
    
    @Column(name = "nombres", length = 100)
    private String nombres;
    
    @Column(name = "apellidos", length = 100)
    private String apellidos;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(length = 20)
    private String telefono;
    
    @Column(length = 200)
    private String direccion;
    
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
    
    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;
    
    @Column(name = "rol", length = 20)
    @Builder.Default
    private String rol = "CLIENTE"; // ADMIN, CLIENTE, etc.
    
    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;
    
    // RELACIÓN CON PEDIDOS
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();
    
    public Usuario(String dni, String nombres, String apellidos, String email, String password) {
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.password = password;
        this.username = email; 
        this.fechaRegistro = LocalDateTime.now();
        this.rol = "CLIENTE";
        this.activo = true;
        this.pedidos = new ArrayList<>();
    }
    
    public String getNombreCompleto() {
        return (nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "");
    }
    
    public void addPedido(Pedido pedido) {
        pedidos.add(pedido);
        pedido.setUsuario(this);
    }
    
    public void removePedido(Pedido pedido) {
        pedidos.remove(pedido);
        pedido.setUsuario(null);
    }
    
    public void actualizarUltimoAcceso() {
        this.ultimoAcceso = LocalDateTime.now();
    }
    
    public boolean esAdmin() {
        return "ADMIN".equalsIgnoreCase(this.rol);
    }
    
    public boolean esCliente() {
        return "CLIENTE".equalsIgnoreCase(this.rol);
    }
    
    public void activar() {
        this.activo = true;
    }
    
    public void desactivar() {
        this.activo = false;
    }
    
    public void cambiarRol(String nuevoRol) {
        if (nuevoRol != null && (nuevoRol.equals("ADMIN") || nuevoRol.equals("CLIENTE"))) {
            this.rol = nuevoRol;
        } else {
            throw new IllegalArgumentException("Rol no válido: " + nuevoRol);
        }
    }
    
    // Método de fábrica para crear admin
    public static Usuario crearAdmin(String dni, String nombres, String apellidos, String email, String password) {
        Usuario admin = new Usuario(dni, nombres, apellidos, email, password);
        admin.setRol("ADMIN");
        return admin;
    }
    
    // Método de fábrica para crear cliente
    public static Usuario crearCliente(String dni, String nombres, String apellidos, String email, String password) {
        return new Usuario(dni, nombres, apellidos, email, password);
    }
}