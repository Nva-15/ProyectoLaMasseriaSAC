    let mesaSeleccionada = null;

    // Cuando se hace clic en una mesa
    document.querySelectorAll('.table').forEach(btn => {
        btn.addEventListener('click', function () {
            mesaSeleccionada = this.getAttribute('data-id');

            // Resaltar la mesa seleccionada visualmente (opcional)
            document.querySelectorAll('.table').forEach(b => b.classList.remove('selected'));
            this.classList.add('selected');
        });
    });

    // Cuando se hace clic en "Continuar"
    document.getElementById('btnContinuar').addEventListener('click', function () {
        if (!mesaSeleccionada) {
            alert("Por favor, selecciona una mesa.");
            return;
        }

        // Colocar el valor seleccionado en el campo de número de mesa
        document.getElementById('campoNumeroMesa').value = mesaSeleccionada;

        // Scroll hacia el formulario
        const formulario = document.querySelector('form');
        formulario.scrollIntoView({ behavior: 'smooth' });
    });
        
    const botonesMesa = document.querySelectorAll(".custom-table");
    const campoMesa = document.getElementById("mesaSeleccionada");
    const campoVisible = document.getElementById("campoNumeroMesa");
    botonesMesa.forEach(btn => {
        btn.addEventListener("click", () => {
            const numeroMesa = btn.getAttribute("data-id");
             campoMesa.value = numeroMesa;
            campoVisible.value = numeroMesa;
            });
        });
        $(function () {
        $('#date').datetimepicker({
            format: 'YYYY-MM-DD'
        });
    });


