import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

/*
 * Avance final (integraciones): la app de Reportes se compila con "npm run build" directamente hacia
 * src/main/resources/static/react-reportes/ del proyecto Spring Boot. Spring la sirve como recurso
 * estático en /react-reportes/assets/reportes.js (nombre fijo, sin hash, para que la plantilla
 * templates/intranet/admin/reportes.html pueda referenciarlo sin manifiesto).
 *
 * "npm run dev" levanta Vite en http://localhost:5173/react-reportes/ y reenvía /api a Spring Boot (8083).
 * Para que la API responda, primero inicie sesión como ADMIN en http://localhost:8083: la cookie de sesión
 * de localhost también se envía a este puerto.
 */
export default defineConfig({
  plugins: [react()],
  base: '/react-reportes/',
  publicDir: false,
  build: {
    outDir: fileURLToPath(new URL('../src/main/resources/static/react-reportes', import.meta.url)),
    emptyOutDir: true,
    rollupOptions: {
      input: fileURLToPath(new URL('./src/main.jsx', import.meta.url)),
      output: {
        entryFileNames: 'assets/reportes.js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name][extname]',
      },
    },
  },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8083' },
  },
});
