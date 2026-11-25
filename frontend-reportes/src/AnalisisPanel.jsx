import { useEffect, useState } from 'react';
import {
  CartesianGrid, ComposedChart, Legend, Line, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { pedirJson, porcentaje, soles } from './api.js';

/*
 * Análisis predictivo del microservicio Python (FastAPI), vía GET /api/reportes/analisis?anio=.
 * Spring Boot responde siempre 200: si Python no está levantado llega {"disponible": false} y aquí se
 * muestra solo un aviso discreto; el resto de Reportes sigue funcionando.
 */
export default function AnalisisPanel({ anio }) {
  const [analisis, setAnalisis] = useState(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    setAnalisis(null);
    pedirJson('/api/reportes/analisis?anio=' + anio)
      .then((a) => { if (vigente) setAnalisis(a); })
      .catch(() => { if (vigente) setAnalisis({ disponible: false, mensaje: 'No se pudo obtener el análisis predictivo.' }); });
    return () => { vigente = false; };
  }, [anio, intento]);

  const encabezado = (
    <div className="card-header bg-dark text-white fw-bold">
      <i className="bi bi-cpu text-info me-2"></i>Análisis predictivo (microservicio Python)
    </div>
  );

  if (analisis === null) {
    return (
      <div className="card shadow-sm border-0 rounded-4 h-100">
        {encabezado}
        <div className="card-body text-muted small"><span className="spinner-border spinner-border-sm me-2"></span>Calculando análisis…</div>
      </div>
    );
  }

  if (!analisis.disponible) {
    return (
      <div className="card shadow-sm border-0 rounded-4 h-100">
        {encabezado}
        <div className="card-body">
          <div className="alert alert-secondary small mb-2">
            <i className="bi bi-info-circle me-1"></i>{analisis.mensaje}
          </div>
          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setIntento((n) => n + 1)}>
            <i className="bi bi-arrow-clockwise me-1"></i>Reintentar
          </button>
        </div>
      </div>
    );
  }

  if (!analisis.hayDatos) {
    return (
      <div className="card shadow-sm border-0 rounded-4 h-100">
        {encabezado}
        <div className="card-body small text-muted">{analisis.mensaje}</div>
      </div>
    );
  }

  const proy = analisis.proyeccion || {};
  const serie = analisis.serie || [];
  const datos = serie.map((s, i) => ({
    etiqueta: s.nombre.substring(0, 3),
    real: s.ingresos,
    proyeccion: proy.disponible && i === serie.length - 1 ? s.ingresos : null,
  }));
  if (proy.disponible) {
    proy.meses.forEach((m) => datos.push({
      etiqueta: m.nombre.substring(0, 3) + (m.anio !== anio ? ' ' + String(m.anio).substring(2) : ''),
      real: null,
      proyeccion: m.ingresoEstimado,
    }));
  }
  const top = analisis.formatoMasRentable;

  return (
    <div className="card shadow-sm border-0 rounded-4 h-100">
      {encabezado}
      <div className="card-body">
        <div className="small text-muted mb-2">Periodo analizado: {analisis.periodo} ({analisis.mesesAnalizados} meses)</div>
        <div className="row g-2 mb-3 small">
          <div className="col-6 col-md-3">
            <div className="border rounded-3 p-2 h-100">
              <div className="text-muted">Crecimiento promedio</div>
              <div className="fw-bold">{porcentaje(analisis.crecimientoPromedio)}</div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="border rounded-3 p-2 h-100">
              <div className="text-muted">Mes pico</div>
              <div className="fw-bold">{analisis.mesPico.nombre}</div>
              <div>{soles(analisis.mesPico.ingresos)}</div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="border rounded-3 p-2 h-100">
              <div className="text-muted">Formato más rentable</div>
              <div className="fw-bold">{top ? top.formato : '—'}</div>
              <div>{top && top.participacion != null ? top.participacion + ' % de ingresos' : ''}</div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="border rounded-3 p-2 h-100">
              <div className="text-muted">Tendencia</div>
              <div className="fw-bold text-capitalize">{proy.disponible ? proy.tendencia : '—'}</div>
              <div>{proy.disponible ? 'R² = ' + proy.r2 + (proy.confiable ? '' : ' (referencial)') : ''}</div>
            </div>
          </div>
        </div>

        <div style={{ height: 220 }}>
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={datos}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="etiqueta" />
              <YAxis />
              <Tooltip formatter={(v) => (v === null ? '—' : soles(v))} />
              <Legend />
              <Line type="monotone" dataKey="real" name="Ingresos reales" stroke="#0d6efd" strokeWidth={2} connectNulls={false} />
              <Line type="monotone" dataKey="proyeccion" name="Proyección lineal" stroke="#fd7e14" strokeWidth={2}
                strokeDasharray="6 4" connectNulls={false} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>

        {proy.disponible
          ? (
            <div className="small mt-2">
              <span className="fw-semibold">Proyección:</span>{' '}
              {proy.meses.map((m) => m.nombre + ' ' + m.anio + ': ' + soles(m.ingresoEstimado)).join(' · ')}
            </div>
          )
          : <div className="small text-muted mt-2">{proy.mensaje}</div>}

        {(analisis.crecimiento || []).length > 0 && (
          <div className="small mt-1">
            <span className="fw-semibold">Crecimiento intermensual:</span>{' '}
            {analisis.crecimiento.map((c) => c.nombre.substring(0, 3) + ' ' + porcentaje(c.tasa)).join(' · ')}
          </div>
        )}

        <div className="fw-semibold small mt-3 mb-1"><i className="bi bi-lightbulb text-warning me-1"></i>Recomendaciones</div>
        <ul className="small mb-0 ps-3">
          {(analisis.recomendaciones || []).map((r) => <li key={r}>{r}</li>)}
        </ul>
      </div>
    </div>
  );
}
