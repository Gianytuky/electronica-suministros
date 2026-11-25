import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';

/*
 * Punto de entrada. La plantilla Thymeleaf reportes.html entrega en el <div id="root"> los años con
 * boletas (data-anios, de ReporteService.aniosDisponibles) y el año seleccionado al inicio (data-anio).
 */
const raiz = document.getElementById('root');
const anios = (raiz.dataset.anios || '')
  .split(',')
  .map((a) => Number(a.trim()))
  .filter((a) => Number.isInteger(a) && a > 0);
const anioInicial = Number(raiz.dataset.anio) || anios[0] || new Date().getFullYear();

createRoot(raiz).render(
  <React.StrictMode>
    <App anios={anios.length ? anios : [anioInicial]} anioInicial={anioInicial} />
  </React.StrictMode>
);
