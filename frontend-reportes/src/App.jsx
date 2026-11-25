import { useEffect, useState } from 'react';
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import AnalisisPanel from './AnalisisPanel.jsx';
import { MESES, PALETA, pedirJson, soles } from './api.js';

/*
 * Reportes de ventas (solo ADMIN).
 *  - GET /api/reportes/ventas-mensuales?anio=  -> 12 filas de SP_Reporte_Ventas
 *  - GET /api/reportes/formatos?anio=          -> ingresos por formato de las OTs cobradas
 *  - GET /api/reportes/analisis?anio=          -> análisis del microservicio Python (se pide aparte, en
 *    AnalisisPanel, para que los gráficos se muestren sin esperar a Python)
 */
function Tarjeta({ icono, color, titulo, valor, detalle }) {
  return (
    <div className="col-sm-6 col-xl-3">
      <div className="card shadow-sm border-0 rounded-4 h-100">
        <div className="card-body d-flex align-items-center gap-3">
          <i className={'bi ' + icono + ' fs-2 ' + color}></i>
          <div>
            <div className="small text-muted">{titulo}</div>
            <div className="fs-5 fw-bold">{valor}</div>
            {detalle && <div className="small text-muted">{detalle}</div>}
          </div>
        </div>
      </div>
    </div>
  );
}

function Caja({ icono, titulo, children, extra }) {
  return (
    <div className="card shadow-sm border-0 rounded-4 h-100">
      <div className="card-header bg-dark text-white fw-bold d-flex justify-content-between align-items-center">
        <span><i className={'bi ' + icono + ' me-2'}></i>{titulo}</span>
        {extra}
      </div>
      {children}
    </div>
  );
}

export default function App({ anios, anioInicial }) {
  const [anio, setAnio] = useState(anioInicial);
  const [ventas, setVentas] = useState([]);
  const [formatos, setFormatos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let vigente = true;
    setCargando(true);
    setError('');
    Promise.all([
      pedirJson('/api/reportes/ventas-mensuales?anio=' + anio),
      pedirJson('/api/reportes/formatos?anio=' + anio),
    ])
      .then(([v, f]) => {
        if (!vigente) return;
        setVentas(v);
        setFormatos(f);
      })
      .catch(() => { if (vigente) setError('No se pudieron cargar los reportes. Recargue la página.'); })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [anio]);

  const datosMes = ventas.map((v) => ({
    mes: MESES[v.mes - 1].substring(0, 3),
    ingresos: Number(v.totalIngresos || 0),
    ordenes: Number(v.cantidadOrdenes || 0),
  }));
  const totalIngresos = datosMes.reduce((a, d) => a + d.ingresos, 0);
  const totalOrdenes = datosMes.reduce((a, d) => a + d.ordenes, 0);
  const mesesConVentas = datosMes.filter((d) => d.ingresos > 0).length;
  const formatosOrdenados = [...formatos]
    .map((f) => ({ ...f, ingresos: Number(f.ingresos || 0) }))
    .sort((a, b) => b.ingresos - a.ingresos);
  const datosTorta = formatosOrdenados.slice(0, 7);
  const otros = formatosOrdenados.slice(7).reduce((a, f) => a + f.ingresos, 0);
  if (otros > 0) datosTorta.push({ formato: 'Otros', ingresos: otros });
  const top = formatosOrdenados[0];

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
        <div>
          <h2 className="fw-bold mb-0"><i className="bi bi-bar-chart-line text-warning me-2"></i>Reportes de ventas</h2>
          <small className="text-muted">
            Vista en React. Datos de <code>SP_Reporte_Ventas</code> y de las OTs cobradas; análisis predictivo
            del microservicio Python.
          </small>
        </div>
        <div className="d-flex align-items-center gap-2">
          <label htmlFor="selAnio" className="fw-semibold mb-0">Año:</label>
          <select id="selAnio" className="form-select" style={{ width: 'auto' }} value={anio}
            onChange={(e) => setAnio(Number(e.target.value))}>
            {anios.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {cargando && <div className="text-muted small mb-2"><span className="spinner-border spinner-border-sm me-2"></span>Actualizando…</div>}

      <div className="row g-3 mb-4">
        <Tarjeta icono="bi-cash-coin" color="text-success" titulo={'Ingresos ' + anio} valor={soles(totalIngresos)} />
        <Tarjeta icono="bi-receipt" color="text-primary" titulo="OTs cobradas" valor={totalOrdenes} />
        <Tarjeta icono="bi-calendar3" color="text-info" titulo="Promedio por mes con ventas"
          valor={soles(mesesConVentas ? totalIngresos / mesesConVentas : 0)} detalle={mesesConVentas + ' mes(es) con ventas'} />
        <Tarjeta icono="bi-trophy" color="text-warning" titulo="Formato más rentable"
          valor={top ? top.formato : '—'} detalle={top ? soles(top.ingresos) : 'Sin ventas'} />
      </div>

      <div className="row g-4 mb-4">
        <div className="col-lg-7">
          <Caja icono="bi-bar-chart-fill text-warning" titulo="Ingresos por mes (S/)">
            <div className="card-body" style={{ height: 320 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={datosMes}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="mes" />
                  <YAxis />
                  <Tooltip formatter={(v) => soles(v)} />
                  <Bar dataKey="ingresos" name="Ingresos" fill="#0d6efd" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Caja>
        </div>
        <div className="col-lg-5">
          <Caja icono="bi-pie-chart-fill text-info" titulo="Ingresos por formato">
            <div className="card-body" style={{ height: 320 }}>
              {datosTorta.length === 0
                ? <div className="text-center text-muted pt-5">Sin ventas en {anio}.</div>
                : (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={datosTorta} dataKey="ingresos" nameKey="formato" innerRadius="45%" outerRadius="80%" paddingAngle={2}>
                        {datosTorta.map((d, i) => <Cell key={d.formato} fill={PALETA[i % PALETA.length]} />)}
                      </Pie>
                      <Tooltip formatter={(v) => soles(v)} />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                )}
            </div>
          </Caja>
        </div>
      </div>

      <div className="row g-4 mb-4">
        <div className="col-lg-5">
          <Caja icono="bi-graph-up text-success" titulo="OTs cobradas por mes">
            <div className="card-body" style={{ height: 300 }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={datosMes}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="mes" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Line type="monotone" dataKey="ordenes" name="OTs cobradas" stroke="#198754" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Caja>
        </div>
        <div className="col-lg-7">
          <AnalisisPanel anio={anio} />
        </div>
      </div>

      <div className="row g-4">
        <div className="col-lg-5">
          <Caja icono="bi-table text-info" titulo={'Resumen mensual de ' + anio}>
            <div className="table-responsive">
              <table className="table table-striped align-middle mb-0">
                <thead className="table-dark small">
                  <tr><th>Mes</th><th className="text-center">OTs cobradas</th><th className="text-end">Ingresos</th></tr>
                </thead>
                <tbody>
                  {ventas.map((v) => (
                    <tr key={v.mes}>
                      <td>{MESES[v.mes - 1]}</td>
                      <td className="text-center">{v.cantidadOrdenes}</td>
                      <td className="text-end">{soles(v.totalIngresos)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="fw-bold table-success">
                    <td>Total del año</td>
                    <td className="text-center">{totalOrdenes}</td>
                    <td className="text-end">{soles(totalIngresos)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </Caja>
        </div>
        <div className="col-lg-7">
          <Caja icono="bi-collection-play text-warning" titulo="Ingresos por formato (OTs finalizadas)">
            <div className="table-responsive">
              <table className="table table-striped table-hover align-middle mb-0">
                <thead className="table-dark small">
                  <tr>
                    <th>Formato</th><th>Categoría</th><th className="text-center">OTs</th>
                    <th className="text-center">Unidades / horas</th><th className="text-end">Ingresos</th>
                  </tr>
                </thead>
                <tbody>
                  {formatosOrdenados.length === 0 && (
                    <tr><td colSpan={5} className="text-center text-muted py-3">Sin OTs cobradas en {anio}.</td></tr>
                  )}
                  {formatosOrdenados.map((f) => (
                    <tr key={f.formato}>
                      <td className="fw-semibold">{f.formato}</td>
                      <td>{f.categoria}</td>
                      <td className="text-center">{f.ordenes}</td>
                      <td className="text-center">{f.unidades}</td>
                      <td className="text-end fw-semibold">{soles(f.ingresos)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Caja>
        </div>
      </div>
    </div>
  );
}
