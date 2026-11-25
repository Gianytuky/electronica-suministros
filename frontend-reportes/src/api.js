/*
 * Utilidades comunes de la app de Reportes.
 * Las llamadas usan la misma sesión (cookie JSESSIONID) que la intranet: /api/reportes/** está protegido
 * por AuthInterceptor y solo responde al ADMIN. Si la sesión venció (401) se vuelve al login.
 */
export const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto',
  'Setiembre', 'Octubre', 'Noviembre', 'Diciembre'];

export const PALETA = ['#0d6efd', '#ffc107', '#198754', '#dc3545', '#0dcaf0', '#6f42c1', '#fd7e14',
  '#20c997', '#6c757d', '#d63384', '#adb5bd', '#343a40', '#8540f5'];

export function soles(valor) {
  return 'S/ ' + Number(valor || 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function porcentaje(valor) {
  if (valor === null || valor === undefined) return '—';
  const n = Number(valor);
  return (n > 0 ? '+' : '') + n.toLocaleString('es-PE', { maximumFractionDigits: 2 }) + ' %';
}

export async function pedirJson(url) {
  const r = await fetch(url, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
  if (r.status === 401) {
    window.location.href = '/login?denegado=true';
    throw new Error('Sesión vencida');
  }
  const tipo = r.headers.get('content-type') || '';
  if (!r.ok || !tipo.includes('application/json')) {
    throw new Error('HTTP ' + r.status);
  }
  return r.json();
}
