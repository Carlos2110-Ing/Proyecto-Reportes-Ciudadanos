/* TÍTULO DINÁMICO DE PESTAÑA - SAFEPERU */

const mensajesFuera = [
    "¡Vuelve, te extrañamos! 🥹",
    "¡SafePeru te espera!",
    "¿Ya nos abandonaste? 👀"
];

const tituloOriginal = document.title;

let intervaloTitulo = null;
let indiceMensaje = 0;

// Detectamos cuándo la página deja de estar visible
document.addEventListener("visibilitychange", () => {

    if (document.hidden) {

        clearInterval(intervaloTitulo);

        indiceMensaje = 0;
        document.title = mensajesFuera[indiceMensaje];

        intervaloTitulo = setInterval(() => {
            indiceMensaje =
                (indiceMensaje + 1) % mensajesFuera.length;

            document.title = mensajesFuera[indiceMensaje];
        }, 3000);

    } else {
        clearInterval(intervaloTitulo);
        intervaloTitulo = null;
        
        document.title = tituloOriginal;
    }
});