document.addEventListener("DOMContentLoaded", function () {
  const btnVerCarrito = document.getElementById("btn-ver-carrito");
  const carrito = document.getElementById("carrito");
  const itemsCarrito = document.getElementById("items-carrito");
  const totalCarrito = document.getElementById("total-carrito");
  const contadorCarrito = document.getElementById("contador-carrito");
  const confirmarBtn = document.querySelector(".btn-success");
  const formularioPago = document.getElementById("formulario-pago");
  const formPedido = document.getElementById("form-pedido");
  const tipoEntregaRadios = document.querySelectorAll('input[name="metodoEntrega"]');
  const campoDireccion = document.getElementById("campo-direccion");
  const campoDireccionForm = document.getElementById("campo-direccion-form");
  const metodoPagoSelect = document.getElementById("metodo-pago");
  const extraPagoDiv = document.getElementById("extra-pago");

  let carritoItems = JSON.parse(localStorage.getItem("carrito")) || [];

  // Mostrar u ocultar carrito
  btnVerCarrito.addEventListener("click", () => {
    carrito.style.display = carrito.style.display === "none" ? "block" : "none";
    actualizarCarrito();
  });

  // Mostrar campo de dirección
  tipoEntregaRadios.forEach(radio => {
    radio.addEventListener("change", () => {
      const mostrar = radio.value === "enviar";
      campoDireccion.style.display = mostrar ? "block" : "none";
      campoDireccionForm.style.display = mostrar ? "block" : "none";
    });
  });

  // Campos dinámicos de pago
  metodoPagoSelect.addEventListener("change", function () {
    extraPagoDiv.innerHTML = "";
    if (this.value === "efectivo") {
      extraPagoDiv.innerHTML = `
        <label for="con-cuanto">¿Con cuánto pagarás?</label>
        <input type="number" class="form-control" id="con-cuanto" placeholder="Ej. 100.00">
      `;
    } else if (this.value === "yape") {
      extraPagoDiv.innerHTML = `
        <label for="yape-ref">Número de operación / Yape</label>
        <input type="text" class="form-control" id="yape-ref" placeholder="Ej. 12345678">
      `;
    } else if (this.value === "tarjeta") {
      extraPagoDiv.innerHTML = `
        <label for="tarjeta-nombre">Nombre en la tarjeta</label>
        <input type="text" class="form-control mb-2" id="tarjeta-nombre">
        <label for="tarjeta-numero">Número de tarjeta</label>
        <input type="text" class="form-control mb-2" id="tarjeta-numero">
        <label for="tarjeta-vencimiento">Vencimiento</label>
        <input type="month" class="form-control" id="tarjeta-vencimiento">
      `;
    }
  });

  // Mostrar formulario al confirmar
  confirmarBtn.addEventListener("click", () => {
    if (carritoItems.length === 0) {
      alert("⚠️ El carrito está vacío.");
      return;
    }
    formularioPago.style.display = "block";
    window.scrollTo({ top: formularioPago.offsetTop, behavior: "smooth" });
  });

  // Enviar el pedido
  formPedido.addEventListener("submit", function (e) {
    e.preventDefault();
    const metodo = document.querySelector('input[name="metodoEntrega"]:checked').value;
    const direccion = metodo === "enviar" ? document.querySelector('#campo-direccion input').value : "";
    const pedido = {
      nombre: document.getElementById("nombre").value,
      telefono: document.getElementById("telefono").value,
      direccion: direccion,
      metodoEntrega: metodo,
      metodoPago: metodoPagoSelect.value,
      total: parseFloat(totalCarrito.textContent.replace("S/ ", "")),
      productos: carritoItems.map(item => ({
        nombre: item.nombre,
        precio: item.precio,
        cantidad: item.cantidad,
        total: (item.precio * item.cantidad).toFixed(2)
      }))
    };

    // Añadir datos extra según método
    if (pedido.metodoPago === "efectivo") {
      pedido.conCuanto = document.getElementById("con-cuanto")?.value || null;
    }
    if (pedido.metodoPago === "yape") {
      pedido.yapeRef = document.getElementById("yape-ref")?.value || null;
    }
    if (pedido.metodoPago === "tarjeta") {
      pedido.tarjeta = {
        nombre: document.getElementById("tarjeta-nombre")?.value || null,
        numero: document.getElementById("tarjeta-numero")?.value || null,
        vencimiento: document.getElementById("tarjeta-vencimiento")?.value || null
      };
    }

    fetch("insertar_pedido.php", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(pedido)
    })
      .then(res => res.json())
      .then(response => {
        if (response.status === "success") {
          mostrarMensajeExito(response.message || "✅ Tu pedido ha sido registrado. En 3 segundos volverás a la página principal...");
          carritoItems = [];
          guardarCarrito();
          
          setTimeout(() => {
            window.location.href = response.redirect || "index.html";
          }, 3000);
        } else {
          alert("❌ Error al procesar el pedido.");
        }
      });
  });

  // Funciones del carrito
  function actualizarCarrito() {
    itemsCarrito.innerHTML = "";
    let total = 0;

    carritoItems.forEach((item, i) => {
      const subtotal = item.precio * item.cantidad;
      total += subtotal;
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${item.nombre}</td><td><img src="${item.imagen}" style="width:60px"></td>
        <td>S/ ${item.precio.toFixed(2)}</td>
        <td><button class="btn btn-sm btn-restar" data-index="${i}">-</button>
        ${item.cantidad}
        <button class="btn btn-sm btn-sumar" data-index="${i}">+</button></td>
        <td>S/ ${subtotal.toFixed(2)}</td>
        <td><button class="btn btn-sm btn-danger btn-eliminar" data-index="${i}">Eliminar</button></td>`;
      itemsCarrito.appendChild(tr);
    });

    totalCarrito.textContent = `S/ ${total.toFixed(2)}`;
    contadorCarrito.textContent = carritoItems.reduce((s, i) => s + i.cantidad, 0);

    document.querySelectorAll(".btn-restar").forEach(b =>
      b.addEventListener("click", () => {
        const i = +b.dataset.index;
        carritoItems[i].cantidad--;
        if (carritoItems[i].cantidad <= 0) carritoItems.splice(i, 1);
        guardarCarrito();
      })
    );
    document.querySelectorAll(".btn-sumar").forEach(b =>
      b.addEventListener("click", () => {
        const i = +b.dataset.index;
        carritoItems[i].cantidad++;
        guardarCarrito();
      })
    );
    document.querySelectorAll(".btn-eliminar").forEach(b =>
      b.addEventListener("click", () => {
        const i = +b.dataset.index;
        carritoItems.splice(i, 1);
        guardarCarrito();
      })
    );
  }

  function guardarCarrito() {
    localStorage.setItem("carrito", JSON.stringify(carritoItems));
    actualizarCarrito();
  }

  actualizarCarrito();
});

function mostrarMensajeExito(msg) {
  document.body.innerHTML = `
    <div style="
      background-color: #d4edda;
      color: #155724;
      padding: 30px;
      text-align: center;
      font-size: 20px;
      font-family: 'Roboto', sans-serif;
      max-width: 600px;
      margin: 100px auto;
      border-radius: 10px;
      border: 1px solid #c3e6cb;
      box-shadow: 0 4px 10px rgba(0,0,0,0.1);
    ">
      ${msg}
    </div>
  `;
}

