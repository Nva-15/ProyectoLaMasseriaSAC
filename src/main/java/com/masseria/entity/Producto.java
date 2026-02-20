package com.masseria.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(length = 500)
    private String descripcion;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
    
    @Column(name = "imagen_url")
    private String imagenUrl;
    
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Categoria categoria;
    
    @Column(name = "stock")
    @Builder.Default
    private Integer stock = 0;
    
    @Column(name = "destacado")
    @Builder.Default
    private Boolean destacado = false;
    
    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;
    
    // Constructor personalizado
    public Producto(String nombre, String descripcion, BigDecimal precio, String imagenUrl, Categoria categoria) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.imagenUrl = imagenUrl;
        this.categoria = categoria;
        this.stock = 0;
        this.destacado = false;
        this.activo = true;
    }
    
    public void reducirStock(Integer cantidad) {
        if (cantidad == null || cantidad < 0) {
            throw new IllegalArgumentException("La cantidad a reducir debe ser positiva");
        }
        if (this.stock < cantidad) {
            throw new IllegalStateException("Stock insuficiente. Stock actual: " + this.stock);
        }
        this.stock -= cantidad;
    }
    
    public void aumentarStock(Integer cantidad) {
        if (cantidad == null || cantidad < 0) {
            throw new IllegalArgumentException("La cantidad a aumentar debe ser positiva");
        }
        this.stock += cantidad;
    }
    
    public boolean tieneStock() {
        return this.stock != null && this.stock > 0;
    }
    
    public boolean tieneStockSuficiente(Integer cantidad) {
        return tieneStock() && this.stock >= cantidad;
    }
    
    public void marcarDestacado() {
        this.destacado = true;
    }
    
    public void quitarDestacado() {
        this.destacado = false;
    }
    
    public void activar() {
        this.activo = true;
    }
    
    public void desactivar() {
        this.activo = false;
    }
    
    public static Producto crearDestacado(String nombre, String descripcion, BigDecimal precio, Categoria categoria) {
        return Producto.builder()
            .nombre(nombre)
            .descripcion(descripcion)
            .precio(precio)
            .categoria(categoria)
            .destacado(true)
            .activo(true)
            .stock(0)
            .build();
    }
}