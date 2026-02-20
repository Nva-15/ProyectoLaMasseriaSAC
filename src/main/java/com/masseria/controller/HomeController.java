package com.masseria.controller;

import com.masseria.entity.Contacto;
import com.masseria.entity.DetallePedido;
import com.masseria.entity.Pedido;
import com.masseria.entity.Producto;
import com.masseria.entity.Reservacion;
import com.masseria.entity.Usuario;
import com.masseria.service.ContactoService;
import com.masseria.service.PedidoService;
import com.masseria.service.ProductoService;
import com.masseria.service.CategoriaService;
import com.masseria.service.ReservacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class HomeController {
    
    @Autowired
    private ProductoService productoService;
    
    @Autowired
    private CategoriaService categoriaService;
    
    @Autowired
    private ContactoService contactoService;
    
    @Autowired
    private ReservacionService reservacionService;
    
    @Autowired
    private PedidoService pedidoService;

    // ========== PÁGINAS PÚBLICAS ==========
    
    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("desayunos", productoService.obtenerPorCategoria("Desayuno"));
        model.addAttribute("bebidasCalientes", productoService.obtenerPorCategoria("Bebida Caliente"));
        model.addAttribute("bebidasFrias", productoService.obtenerPorCategoria("Bebida Fría"));
        model.addAttribute("postres", productoService.obtenerPorCategoria("Postre"));
        model.addAttribute("destacados", productoService.obtenerDestacados());
        model.addAttribute("novedades", productoService.obtenerTodos());
        model.addAttribute("categorias", categoriaService.obtenerTodas());
        model.addAttribute("activePage", "inicio");
        return "index";
    }

    @GetMapping("/nosotros")
    public String nosotros(Model model) {
        model.addAttribute("activePage", "nosotros");
        return "nosotros";
    }

    @GetMapping("/contacto")
    public String contacto(Model model) {
        model.addAttribute("activePage", "contacto");
        return "contacto";
    }

    // ========== PÁGINAS PROTEGIDAS ==========

    @GetMapping("/reservaciones")
    public String reservaciones(HttpSession session, Model model, RedirectAttributes redirectAttributes,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String estado) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión para hacer una reservación");
            return "redirect:/login?acceso=denegado";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("nombreUsuario", usuario.getNombres() + " " + usuario.getApellidos());
        model.addAttribute("emailUsuario", usuario.getEmail());
        model.addAttribute("telefonoUsuario", usuario.getTelefono() != null ? usuario.getTelefono() : "");
        model.addAttribute("fechaMinima", LocalDate.now().toString());

        List<String> horarios = List.of(
            "09:00", "10:00", "11:00", "12:00", "13:00", "14:00",
            "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00"
        );
        model.addAttribute("horarios", horarios);
        model.addAttribute("activePage", "reservaciones");

        // ===== HISTORIAL CON FILTROS =====
        boolean tieneFechas = (fechaInicio != null && !fechaInicio.isEmpty())
                           || (fechaFin    != null && !fechaFin.isEmpty());
        boolean tieneEstado = (estado != null && !estado.isEmpty());

        String periodoActivo;
        if (tieneFechas || tieneEstado) {
            periodoActivo = "todas";
        } else {
            periodoActivo = (periodo != null && !periodo.isEmpty()) ? periodo : "proximas";
        }

        LocalDate fechaInicioDate = null;
        LocalDate fechaFinDate    = null;
        if (fechaInicio != null && !fechaInicio.isEmpty()) fechaInicioDate = LocalDate.parse(fechaInicio);
        if (fechaFin    != null && !fechaFin.isEmpty())    fechaFinDate    = LocalDate.parse(fechaFin);

        model.addAttribute("historialReservaciones",
                reservacionService.obtenerHistorialPorUsuario(
                        usuario.getId(), periodoActivo, fechaInicioDate, fechaFinDate, estado));
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("fechaInicio",   fechaInicio != null ? fechaInicio : "");
        model.addAttribute("fechaFin",      fechaFin    != null ? fechaFin    : "");
        model.addAttribute("estadoFiltro",  estado      != null ? estado      : "");

        return "reservacion";
    }

    @PostMapping("/reservaciones/{id}/cancelar")
    public String cancelarMiReservacion(@PathVariable Long id,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        try {
            reservacionService.obtenerPorId(id).ifPresent(res -> {
                if (res.getUsuario() != null
                        && res.getUsuario().getId().equals(usuario.getId())
                        && "PENDIENTE".equals(res.getEstado())) {
                    reservacionService.cancelarReservacion(id);
                }
            });
            redirectAttributes.addFlashAttribute("mensaje", "Reservación cancelada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo cancelar la reservación");
        }
        return "redirect:/reservaciones?tab=historial";
    }

    @GetMapping("/pedidos")
    public String pedidos(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String nombreProducto,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión para hacer un pedido");
            return "redirect:/login?acceso=denegado";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("nombreCompleto", usuario.getNombres() + " " + usuario.getApellidos());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
        model.addAttribute("direccion", usuario.getDireccion() != null ? usuario.getDireccion() : "");

        // Historial de pedidos del usuario con filtros
        boolean tieneFechas   = (fechaInicio != null && !fechaInicio.isEmpty())
                             || (fechaFin    != null && !fechaFin.isEmpty());
        boolean tieneProducto = (nombreProducto != null && !nombreProducto.isEmpty());

        // Si el usuario usa rango de fechas o busca por producto,
        // forzar "todos" para no limitar el período automáticamente.
        String periodoActivo;
        if (tieneFechas || tieneProducto) {
            periodoActivo = "todos";
        } else {
            periodoActivo = (periodo != null && !periodo.isEmpty()) ? periodo : "hoy";
        }

        LocalDate fechaInicioDate = null;
        LocalDate fechaFinDate = null;
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            fechaInicioDate = LocalDate.parse(fechaInicio);
        }
        if (fechaFin != null && !fechaFin.isEmpty()) {
            fechaFinDate = LocalDate.parse(fechaFin);
        }
        List<Pedido> historialPedidos = pedidoService.obtenerHistorialPorUsuario(
                usuario.getId(), periodoActivo, fechaInicioDate, fechaFinDate, nombreProducto);

        model.addAttribute("historialPedidos", historialPedidos);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("fechaInicio", fechaInicio != null ? fechaInicio : "");
        model.addAttribute("fechaFin", fechaFin != null ? fechaFin : "");
        model.addAttribute("nombreProducto", nombreProducto != null ? nombreProducto : "");
        model.addAttribute("activePage", "pedidos");
        return "pedidos";
    }

    // ========== PROCESAMIENTO DE RESERVACIONES ==========
    
    @PostMapping("/reservaciones/guardar")
    public String guardarReservacion(
            HttpSession session,
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String telefono,
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam Integer cantidadPersonas,
            @RequestParam(required = false) String notas,
            RedirectAttributes redirectAttributes) {
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión");
            return "redirect:/login";
        }
        
        try {
            Reservacion reservacion = new Reservacion();
            reservacion.setNombre(usuario.getNombres() + " " + usuario.getApellidos());
            reservacion.setEmail(usuario.getEmail());
            reservacion.setTelefono(telefono);
            reservacion.setFecha(LocalDate.parse(fecha));
            reservacion.setHora(LocalTime.parse(hora));
            reservacion.setCantidadPersonas(cantidadPersonas);
            reservacion.setNotas(notas);
            reservacion.setUsuario(usuario);
            reservacion.setEstado("PENDIENTE");
            
            if (!reservacionService.verificarDisponibilidad(LocalDate.parse(fecha), LocalTime.parse(hora))) {
                redirectAttributes.addFlashAttribute("error", "Ese horario ya no está disponible");
                return "redirect:/reservaciones?error=horario";
            }
            
            reservacionService.guardar(reservacion);
            redirectAttributes.addFlashAttribute("mensaje", "¡Reservación realizada con éxito!");
            return "redirect:/reservaciones?exito";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la reservación");
            return "redirect:/reservaciones?error";
        }
    }

    // ===== MÉTODO CORREGIDO PARA GUARDAR PEDIDOS CON DETALLES =====
    @PostMapping("/pedidos/guardar")
    @ResponseBody
    public Map<String, Object> guardarPedido(
            HttpSession session,
            @RequestParam String cliente,
            @RequestParam String correo,
            @RequestParam String telefono,
            @RequestParam String total,
            @RequestParam String metodoEntrega,
            @RequestParam(required = false) String direccion,
            @RequestParam String metodoPago,
            @RequestParam(required = false) String notas,
            @RequestParam String detalles,
            RedirectAttributes redirectAttributes) {
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return Map.of("success", false, "message", "Debes iniciar sesión");
        }
        
        try {
            // Crear el pedido
            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            pedido.setClienteNombre(cliente);
            pedido.setClienteEmail(correo);
            pedido.setClienteTelefono(telefono);
            pedido.setTipoEntrega(metodoEntrega);
            pedido.setMetodoPago(metodoPago);
            pedido.setNotas(notas);
            pedido.setTotal(new BigDecimal(total));
            pedido.setEstado(PedidoService.ESTADO_PENDIENTE);
            pedido.setFechaPedido(LocalDateTime.now());
            
            // Guardar dirección según tipo de entrega
            if ("DOMICILIO".equals(metodoEntrega) && direccion != null && !direccion.isEmpty()) {
                pedido.setDireccionEntrega(direccion);
            } else {
                pedido.setDireccionEntrega("RECOGER EN TIENDA");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> detallesList = mapper.readValue(detalles, new TypeReference<List<Map<String, Object>>>() {});
            
            // Crear y agregar cada detalle al pedido
            for (Map<String, Object> item : detallesList) {
                Long productoId = Long.parseLong(item.get("id").toString());
                Integer cantidad = Integer.parseInt(item.get("cantidad").toString());
                
                Producto producto = productoService.obtenerPorId(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productoId));
                
                DetallePedido detalle = new DetallePedido();
                detalle.setProducto(producto);
                detalle.setCantidad(cantidad);
                detalle.setPrecioUnitario(producto.getPrecio());
                detalle.setSubtotal(producto.getPrecio().multiply(new BigDecimal(cantidad)));
                
                pedido.addDetalle(detalle);
            }
            
            Pedido pedidoGuardado = pedidoService.guardar(pedido);
            
            System.out.println(" Pedido guardado en BD - ID: " + pedidoGuardado.getId());
            System.out.println("   Usuario: " + usuario.getEmail());
            System.out.println("   Total: S/ " + total);
            System.out.println("   Detalles: " + pedidoGuardado.getDetalles().size() + " productos");
            
            return Map.of("success", true, "message", "Pedido guardado correctamente");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Error al guardar el pedido: " + e.getMessage());
        }
    }

    // ========== FORMULARIO DE CONTACTO ==========
    
    @PostMapping("/mensaje")
    public String enviarMensaje(
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String asunto,
            @RequestParam String mensaje,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Contacto contacto = new Contacto();
        contacto.setNombre(nombre);
        contacto.setEmail(email);
        contacto.setAsunto(asunto);
        contacto.setMensaje(mensaje);
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            contacto.setUsuario(usuario);
            contacto.setEmail(usuario.getEmail());
        }
        
        contactoService.guardar(contacto);
        
        redirectAttributes.addFlashAttribute("mensaje", "Mensaje enviado con éxito");
        return "redirect:/contacto?enviado";
    }
}