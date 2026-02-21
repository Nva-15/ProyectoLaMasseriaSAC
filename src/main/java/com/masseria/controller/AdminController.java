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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
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
    public String listarPedidos(HttpSession session, Model model, RedirectAttributes redirectAttributes,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String clienteNombre,
            @RequestParam(required = false) String estado) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        boolean tieneFechas = (fechaInicio != null && !fechaInicio.isEmpty())
                           || (fechaFin    != null && !fechaFin.isEmpty());
        String periodoActivo = tieneFechas ? "todos"
                : (periodo != null && !periodo.isEmpty()) ? periodo : "hoy";

        LocalDateTime inicio = null;
        LocalDateTime fin    = null;
        if (tieneFechas) {
            if (fechaInicio != null && !fechaInicio.isEmpty())
                inicio = LocalDate.parse(fechaInicio).atStartOfDay();
            if (fechaFin != null && !fechaFin.isEmpty())
                fin = LocalDate.parse(fechaFin).atTime(LocalTime.MAX);
        } else {
            fin = LocalDateTime.now();
            switch (periodoActivo) {
                case "hoy"   -> inicio = LocalDate.now().atStartOfDay();
                case "7dias" -> inicio = LocalDate.now().minusDays(7).atStartOfDay();
                case "1mes"  -> inicio = LocalDate.now().minusMonths(1).atStartOfDay();
                default      -> { inicio = null; fin = null; }
            }
        }

        final LocalDateTime fInicio  = inicio;
        final LocalDateTime fFin     = fin;
        final String        fEstado  = (estado        != null && !estado.isEmpty())        ? estado        : null;
        final String        fCliente = (clienteNombre != null && !clienteNombre.isEmpty()) ? clienteNombre.toLowerCase() : null;

        List<Pedido> pedidos = pedidoService.obtenerTodos().stream()
                .filter(p -> fInicio  == null || (p.getFechaPedido() != null && !p.getFechaPedido().isBefore(fInicio)))
                .filter(p -> fFin     == null || (p.getFechaPedido() != null && !p.getFechaPedido().isAfter(fFin)))
                .filter(p -> fEstado  == null || fEstado.equals(p.getEstado()))
                .filter(p -> fCliente == null || (p.getClienteNombre() != null
                                                  && p.getClienteNombre().toLowerCase().contains(fCliente)))
                .sorted(Comparator.comparing(Pedido::getFechaPedido,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        model.addAttribute("activePage",    "admin-pedidos");
        model.addAttribute("pedidos",       pedidos);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("fechaInicio",   fechaInicio   != null ? fechaInicio   : "");
        model.addAttribute("fechaFin",      fechaFin      != null ? fechaFin      : "");
        model.addAttribute("clienteNombre", clienteNombre != null ? clienteNombre : "");
        model.addAttribute("estadoFiltro",  estado        != null ? estado        : "");
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
    public String listarReservaciones(HttpSession session, Model model, RedirectAttributes redirectAttributes,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String clienteNombre) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        boolean tieneFiltros = (fechaInicio   != null && !fechaInicio.isEmpty())
                            || (fechaFin      != null && !fechaFin.isEmpty())
                            || (estado        != null && !estado.isEmpty())
                            || (clienteNombre != null && !clienteNombre.isEmpty());

        LocalDate inicioDate = null;
        LocalDate finDate    = null;
        if (fechaInicio != null && !fechaInicio.isEmpty()) inicioDate = LocalDate.parse(fechaInicio);
        if (fechaFin    != null && !fechaFin.isEmpty())    finDate    = LocalDate.parse(fechaFin);
        if (!tieneFiltros) inicioDate = LocalDate.now();   // default: hoy en adelante

        final LocalDate fInicio  = inicioDate;
        final LocalDate fFin     = finDate;
        final String    fEstado  = (estado        != null && !estado.isEmpty())        ? estado        : null;
        final String    fCliente = (clienteNombre != null && !clienteNombre.isEmpty()) ? clienteNombre.toLowerCase() : null;

        List<Reservacion> reservaciones = reservacionService.obtenerTodas().stream()
                .filter(r -> fInicio  == null || (r.getFecha() != null && !r.getFecha().isBefore(fInicio)))
                .filter(r -> fFin     == null || (r.getFecha() != null && !r.getFecha().isAfter(fFin)))
                .filter(r -> fEstado  == null || fEstado.equals(r.getEstado()))
                .filter(r -> fCliente == null || (r.getNombre() != null
                                                  && r.getNombre().toLowerCase().contains(fCliente)))
                .sorted(Comparator.comparing(Reservacion::getFecha,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Reservacion::getHora,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        model.addAttribute("activePage",    "admin-reservaciones");
        model.addAttribute("reservaciones", reservaciones);
        model.addAttribute("fechaInicio",   fechaInicio   != null ? fechaInicio   : "");
        model.addAttribute("fechaFin",      fechaFin      != null ? fechaFin      : "");
        model.addAttribute("estadoFiltro",  estado        != null ? estado        : "");
        model.addAttribute("clienteNombre", clienteNombre != null ? clienteNombre : "");
        model.addAttribute("tieneFiltros",  tieneFiltros);
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
    public String listarProductos(HttpSession session, Model model, RedirectAttributes redirectAttributes,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String activoFiltro,
            @RequestParam(required = false) String destacadoFiltro) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        Boolean fActivo    = "true".equals(activoFiltro)    ? Boolean.TRUE  : "false".equals(activoFiltro)    ? Boolean.FALSE : null;
        Boolean fDestacado = "true".equals(destacadoFiltro) ? Boolean.TRUE  : "false".equals(destacadoFiltro) ? Boolean.FALSE : null;

        List<Producto> productos = productoService.obtenerTodosAdmin().stream()
                .filter(p -> categoriaId == null || (p.getCategoria() != null && categoriaId.equals(p.getCategoria().getId())))
                .filter(p -> fActivo     == null || fActivo.equals(p.getActivo()))
                .filter(p -> fDestacado  == null || fDestacado.equals(p.getDestacado()))
                .toList();

        model.addAttribute("activePage",       "admin-productos");
        model.addAttribute("productos",        productos);
        model.addAttribute("categorias",       categoriaService.obtenerTodas());
        model.addAttribute("categoriaFiltro",  categoriaId);
        model.addAttribute("activoFiltro",     activoFiltro     != null ? activoFiltro     : "");
        model.addAttribute("destacadoFiltro",  destacadoFiltro  != null ? destacadoFiltro  : "");
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
    public String listarUsuarios(HttpSession session, Model model, RedirectAttributes redirectAttributes,
            @RequestParam(required = false) String estadoFiltro,
            @RequestParam(required = false) String limite) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        // Defaults: solo activos, últimos 10
        String estado = (estadoFiltro != null && !estadoFiltro.isEmpty()) ? estadoFiltro : "activos";
        int limiteNum  = "todos".equals(limite) ? Integer.MAX_VALUE
                       : "200".equals(limite)   ? 200
                       : "50".equals(limite)    ? 50
                       : 10; // default

        Boolean fActivo = "activos".equals(estado)   ? Boolean.TRUE
                        : "inactivos".equals(estado) ? Boolean.FALSE
                        : null; // "todos"

        // Admins: todos siempre (suelen ser pocos)
        List<Usuario> admins = usuarioService.obtenerPorRol("ADMIN").stream()
                .filter(u -> fActivo == null || fActivo.equals(u.getActivo()))
                .sorted(Comparator.comparing(Usuario::getFechaRegistro,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // Total real de clientes sin aplicar filtro de estado (para el denominador del contador)
        List<Usuario> todosClientesSinFiltro = usuarioService.obtenerPorRol("CLIENTE");
        long totalClientesAll = todosClientesSinFiltro.size();

        // Clientes: filtro estado + límite, ordenados por registro DESC
        List<Usuario> todosClientesFiltrados = todosClientesSinFiltro.stream()
                .filter(u -> fActivo == null || fActivo.equals(u.getActivo()))
                .sorted(Comparator.comparing(Usuario::getFechaRegistro,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Usuario> clientes = limiteNum < todosClientesFiltrados.size()
                ? todosClientesFiltrados.subList(0, limiteNum)
                : todosClientesFiltrados;

        model.addAttribute("activePage",       "admin-usuarios");
        model.addAttribute("admins",           admins);
        model.addAttribute("clientes",         clientes);
        model.addAttribute("totalClientesAll", totalClientesAll);
        model.addAttribute("estadoFiltro",     estado);
        model.addAttribute("limite",           limite != null ? limite : "10");
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

    @GetMapping("/usuarios/{id}/editar")
    public String editarUsuario(@PathVariable Long id, HttpSession session, Model model,
                                 RedirectAttributes redirectAttributes) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        return usuarioService.obtenerPorId(id)
                .map(usuario -> {
                    model.addAttribute("activePage", "admin-usuarios");
                    model.addAttribute("usuario", usuario);
                    return "admin/usuario-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                    return "redirect:/admin/usuarios";
                });
    }

    @PostMapping("/usuarios/{id}/editar")
    public String guardarEdicionUsuario(@PathVariable Long id,
                                         @RequestParam(required = false) String nombres,
                                         @RequestParam(required = false) String apellidos,
                                         @RequestParam(required = false) String email,
                                         @RequestParam(required = false) String telefono,
                                         @RequestParam(required = false) String direccion,
                                         @RequestParam(required = false) String dni,
                                         @RequestParam(required = false) String nuevaPassword,
                                         @RequestParam(required = false) String confirmarPassword,
                                         RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = verificarAdmin(session, redirectAttributes);
        if (redirect != null) return redirect;

        try {
            if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()
                    && !nuevaPassword.equals(confirmarPassword)) {
                redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
                return "redirect:/admin/usuarios/" + id + "/editar";
            }
            usuarioService.actualizarDatos(id, nombres, apellidos, email, telefono, direccion, dni, nuevaPassword);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario actualizado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/usuarios/" + id + "/editar";
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