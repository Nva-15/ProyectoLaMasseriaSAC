package com.masseria.controller;

import com.masseria.entity.*;
import com.masseria.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ReservacionService reservacionService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ContactoService contactoService;

    @Autowired
    private CategoriaService categoriaService;
    
    @Autowired
    private ImagenService imagenService;

    // ==================== VERIFICACIÓN ADMIN ====================

    private String verificarAdmin(HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuario");
        if (usuarioSesion == null) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión");
            return "redirect:/login";
        }
        // Recargar usuario desde la BD para tener el rol actualizado
        Usuario usuarioDB = usuarioService.obtenerPorId(usuarioSesion.getId()).orElse(null);
        if (usuarioDB == null) {
            session.invalidate();
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/login";
        }
        // Actualizar la sesión con los datos frescos de la BD
        session.setAttribute("usuario", usuarioDB);
        if (!usuarioDB.esAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado. Se requiere rol de administrador.");
            return "redirect:/";
        }
        return null;
    }

    // ==================== DASHBOARD ====================

    @GetMapping("")
    public String dashboard(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-dashboard");

        // Estadísticas
        model.addAttribute("pedidosPendientes", pedidoService.contarPedidosPendientes());
        model.addAttribute("ventasHoy", pedidoService.calcularTotalVentasHoy());
        model.addAttribute("pedidosHoy", pedidoService.contarPedidosHoy());
        model.addAttribute("reservacionesHoy", reservacionService.obtenerReservacionesDelDia(LocalDate.now()).size());
        model.addAttribute("mensajesNoLeidos", contactoService.obtenerNoLeidos().size());
        model.addAttribute("totalUsuarios", usuarioService.obtenerTodos().size());
        model.addAttribute("totalProductos", productoService.obtenerTodosAdmin().size());
        model.addAttribute("totalCategorias", categoriaService.contarCategorias());

        // Últimos pedidos y próximas reservaciones
        List<Pedido> ultimosPedidos = pedidoService.obtenerTodos();
        model.addAttribute("ultimosPedidos", ultimosPedidos.stream().limit(5).toList());
        model.addAttribute("proximasReservaciones", reservacionService.obtenerProximasReservaciones().stream().limit(5).toList());

        return "admin/dashboard";
    }

    // ==================== PEDIDOS ====================

    @GetMapping("/pedidos")
    public String listarPedidos(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-pedidos");
        model.addAttribute("pedidos", pedidoService.obtenerTodos());
        return "admin/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String verPedido(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        return pedidoService.obtenerPorId(id)
                .map(pedido -> {
                    model.addAttribute("activePage", "admin-pedidos");
                    model.addAttribute("pedido", pedido);
                    return "admin/pedido-detalle";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Pedido no encontrado");
                    return "redirect:/admin/pedidos";
                });
    }

    @PostMapping("/pedidos/{id}/estado")
    public String cambiarEstadoPedido(@PathVariable Long id, @RequestParam String estado,
                                       RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            pedidoService.actualizarEstado(id, estado);
            redirectAttributes.addFlashAttribute("mensaje", "Estado del pedido actualizado a: " + estado);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar estado: " + e.getMessage());
        }
        return "redirect:/admin/pedidos/" + id;
    }

    // ==================== RESERVACIONES ====================

    @GetMapping("/reservaciones")
    public String listarReservaciones(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-reservaciones");
        model.addAttribute("reservaciones", reservacionService.obtenerTodas());
        return "admin/reservaciones";
    }

    @PostMapping("/reservaciones/{id}/confirmar")
    public String confirmarReservacion(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            reservacionService.confirmarReservacion(id);
            redirectAttributes.addFlashAttribute("mensaje", "Reservación confirmada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/reservaciones";
    }

    @PostMapping("/reservaciones/{id}/cancelar")
    public String cancelarReservacion(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            reservacionService.cancelarReservacion(id);
            redirectAttributes.addFlashAttribute("mensaje", "Reservación cancelada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/reservaciones";
    }

    // ==================== PRODUCTOS ====================

    @GetMapping("/productos")
    public String listarProductos(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-productos");
        model.addAttribute("productos", productoService.obtenerTodosAdmin());
        return "admin/productos";
    }

    @GetMapping("/productos/nuevo")
    public String nuevoProducto(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-productos");
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaService.obtenerTodas());
        model.addAttribute("titulo", "Nuevo Producto");
        return "admin/producto-form";
    }

    @GetMapping("/productos/{id}/editar")
    public String editarProducto(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        return productoService.obtenerPorId(id)
                .map(producto -> {
                    model.addAttribute("activePage", "admin-productos");
                    model.addAttribute("producto", producto);
                    model.addAttribute("categorias", categoriaService.obtenerTodas());
                    model.addAttribute("titulo", "Editar Producto");
                    return "admin/producto-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Producto no encontrado");
                    return "redirect:/admin/productos";
                });
    }

    @PostMapping("/productos/guardar")
    public String guardarProducto(@RequestParam String nombre,
                                   @RequestParam(required = false) String descripcion,
                                   @RequestParam BigDecimal precio,
                                   @RequestParam(required = false) MultipartFile imagen,
                                   @RequestParam Long categoriaId,
                                   @RequestParam(defaultValue = "0") Integer stock,
                                   @RequestParam(defaultValue = "false") Boolean destacado,
                                   @RequestParam(defaultValue = "true") Boolean activo,
                                   @RequestParam(required = false) Long id,
                                   @RequestParam(required = false) String imagenUrlExistente,
                                   RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            Producto producto;
            if (id != null) {
                producto = productoService.obtenerPorId(id)
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            } else {
                producto = new Producto();
            }

            producto.setNombre(nombre);
            producto.setDescripcion(descripcion);
            producto.setPrecio(precio);
            producto.setStock(stock);
            producto.setDestacado(destacado);
            producto.setActivo(activo);

            Categoria categoria = categoriaService.obtenerPorId(categoriaId)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            producto.setCategoria(categoria);

            // MANEJO DE LA IMAGEN
            System.out.println("=== PROCESANDO IMAGEN ===");
            System.out.println("ID Producto: " + id);
            System.out.println("Nombre: " + nombre);
            System.out.println("Categoría: " + categoria.getNombre());
            System.out.println("Imagen recibida: " + (imagen != null ? imagen.getOriginalFilename() : "NO"));
            System.out.println("Imagen vacía? " + (imagen != null ? imagen.isEmpty() : "null"));
            System.out.println("Imagen existente: " + imagenUrlExistente);

            if (imagen != null && !imagen.isEmpty()) {
                try {
                    // Guardar nueva imagen
                    String rutaImagen = imagenService.guardarImagen(imagen, categoria.getNombre(), nombre);
                    producto.setImagenUrl(rutaImagen);
                    System.out.println("NUEVA RUTA GUARDADA: " + rutaImagen);
                } catch (Exception e) {
                    System.err.println("ERROR AL GUARDAR IMAGEN: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else if (imagenUrlExistente != null && !imagenUrlExistente.isEmpty()) {
                // Conservar imagen existente
                producto.setImagenUrl(imagenUrlExistente);
                System.out.println("CONSERVANDO RUTA EXISTENTE: " + imagenUrlExistente);
            } else {
                // No hay imagen
                producto.setImagenUrl(null);
                System.out.println("SIN IMAGEN - se guarda null");
            }
            System.out.println("=========================");

            Producto productoGuardado = productoService.guardar(producto);
            System.out.println("PRODUCTO GUARDADO EN BD - ID: " + productoGuardado.getId() + ", Imagen: " + productoGuardado.getImagenUrl());

            redirectAttributes.addFlashAttribute("mensaje", id != null ? "Producto actualizado exitosamente" : "Producto creado exitosamente");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al guardar el producto: " + e.getMessage());
        }
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/{id}/eliminar")
    public String eliminarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            productoService.obtenerPorId(id).ifPresent(producto -> {
                producto.setActivo(false);
                productoService.guardar(producto);
            });
            redirectAttributes.addFlashAttribute("mensaje", "Producto desactivado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/productos";
    }

    // ==================== USUARIOS ====================

    @GetMapping("/usuarios")
    public String listarUsuarios(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-usuarios");
        model.addAttribute("admins", usuarioService.obtenerPorRol("ADMIN"));
        model.addAttribute("clientes", usuarioService.obtenerPorRol("CLIENTE"));
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRol(@PathVariable Long id, @RequestParam String rol,
                              RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            usuarioService.cambiarRol(id, rol);
            redirectAttributes.addFlashAttribute("mensaje", "Rol actualizado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstadoUsuario(@PathVariable Long id, @RequestParam boolean activo,
                                        RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            usuarioService.activarDesactivar(id, activo);
            redirectAttributes.addFlashAttribute("mensaje", activo ? "Usuario activado" : "Usuario desactivado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ==================== CONTACTOS ====================

    @GetMapping("/contactos")
    public String listarContactos(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-contactos");
        model.addAttribute("contactos", contactoService.obtenerTodosOrdenados());
        return "admin/contactos";
    }

    @GetMapping("/contactos/{id}")
    public String verContacto(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        return contactoService.obtenerPorId(id)
                .map(contacto -> {
                    contactoService.marcarComoLeido(id);
                    model.addAttribute("activePage", "admin-contactos");
                    model.addAttribute("contacto", contacto);
                    return "admin/contacto-detalle";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Mensaje no encontrado");
                    return "redirect:/admin/contactos";
                });
    }

    @PostMapping("/contactos/{id}/eliminar")
    public String eliminarContacto(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            contactoService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Mensaje eliminado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/contactos";
    }

    // ==================== CATEGORÍAS ====================

    @GetMapping("/categorias")
    public String listarCategorias(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-categorias");
        List<Categoria> categorias = categoriaService.obtenerTodas();
        model.addAttribute("categorias", categorias);

        Map<Long, Integer> productosCount = new HashMap<>();
        for (Categoria cat : categorias) {
            int count = productoService.obtenerPorCategoria(cat.getNombre()).size();
            productosCount.put(cat.getId(), count);
        }
        model.addAttribute("productosCount", productosCount);

        return "admin/categorias";
    }

    @GetMapping("/categorias/nueva")
    public String nuevaCategoria(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        model.addAttribute("activePage", "admin-categorias");
        model.addAttribute("categoria", new Categoria());
        model.addAttribute("titulo", "Nueva Categoría");
        return "admin/categoria-form";
    }

    @GetMapping("/categorias/{id}/editar")
    public String editarCategoria(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        return categoriaService.obtenerPorId(id)
                .map(cat -> {
                    model.addAttribute("activePage", "admin-categorias");
                    model.addAttribute("categoria", cat);
                    model.addAttribute("titulo", "Editar Categoría");
                    return "admin/categoria-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Categoría no encontrada");
                    return "redirect:/admin/categorias";
                });
    }

    @PostMapping("/categorias/guardar")
    public String guardarCategoria(@RequestParam String nombre,
                                    @RequestParam(required = false) String descripcion,
                                    @RequestParam(required = false) Long id,
                                    RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            if (id != null) {
                Categoria catActualizada = new Categoria();
                catActualizada.setNombre(nombre);
                catActualizada.setDescripcion(descripcion);
                categoriaService.actualizar(id, catActualizada);
                redirectAttributes.addFlashAttribute("mensaje", "Categoría actualizada exitosamente");
            } else {
                Categoria nueva = new Categoria(nombre, descripcion);
                categoriaService.guardar(nueva);
                redirectAttributes.addFlashAttribute("mensaje", "Categoría creada exitosamente");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/{id}/eliminar")
    public String eliminarCategoria(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            categoriaService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Categoría eliminada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/categorias";
    }
}