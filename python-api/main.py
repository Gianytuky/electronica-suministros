"""
Microservicio de análisis estadístico - Electrónica y Suministros S.A.C. (avance final, integraciones).

Spring Boot (ReporteService.analisis) envía el historial de ventas de un año:
    POST /api/analisis
    {
      "anio": 2026,
      "mesActual": 11,                       # meses posteriores aún no ocurren y no se analizan
      "ventasMensuales": [{"mes": 1, "cantidadOrdenes": 3, "totalIngresos": 450.0}, ...],   # 12 filas
      "formatos": [{"formato": "VHS", "categoria": "Video", "ordenes": 5, "unidades": 10, "ingresos": 250.0}, ...]
    }
y recibe: crecimiento intermensual, proyección lineal de los próximos meses, mes pico, formato más rentable
y recomendaciones ejecutivas.

Ultra ligero: solo FastAPI + Uvicorn y la librería estándar (statistics.linear_regression, Python 3.10+).
No se conecta a la base de datos: todo lo que necesita llega en la petición.

Ejecución (desde esta carpeta):
    pip install -r requirements.txt
    uvicorn main:app --port 8000
Documentación interactiva: http://localhost:8000/docs
"""

from statistics import StatisticsError, fmean, linear_regression
from typing import List, Optional

from fastapi import FastAPI
from pydantic import BaseModel, Field

MESES = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
         "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"]
MESES_PARA_PROYECTAR = 3
MINIMO_MESES_PROYECCION = 3


class VentaMensual(BaseModel):
    mes: int = Field(ge=1, le=12)
    cantidadOrdenes: int = 0
    totalIngresos: float = 0.0


class IngresoFormato(BaseModel):
    formato: str
    categoria: Optional[str] = None
    ordenes: int = 0
    unidades: float = 0
    ingresos: float = 0.0


class SolicitudAnalisis(BaseModel):
    anio: int = Field(ge=2000, le=2100)
    mesActual: int = Field(default=12, ge=1, le=12)
    ventasMensuales: List[VentaMensual] = []
    formatos: List[IngresoFormato] = []


app = FastAPI(title="Análisis de ventas - Electrónica y Suministros", version="1.0.0")


@app.get("/")
def estado():
    return {"servicio": "analisis-ventas", "estado": "activo"}


@app.post("/api/analisis")
def analizar(solicitud: SolicitudAnalisis):
    return calcular_analisis(solicitud)


# ----------------------------------------------------------------------------------------------
# Lógica de negocio (funciones puras, sin FastAPI, para poder probarlas por separado)
# ----------------------------------------------------------------------------------------------

def calcular_analisis(s: SolicitudAnalisis) -> dict:
    ingresos_por_mes = {m: 0.0 for m in range(1, 13)}
    ordenes_por_mes = {m: 0 for m in range(1, 13)}
    for v in s.ventasMensuales:
        ingresos_por_mes[v.mes] = float(v.totalIngresos or 0)
        ordenes_por_mes[v.mes] = int(v.cantidadOrdenes or 0)

    # Meses analizados: del primer al último mes con ingresos, sin pasar del mes actual (o diciembre si el año
    # ya terminó). Los meses finales sin boletas se excluyen: suelen ser el mes en curso aún sin cobros.
    con_ingresos = [m for m in range(1, s.mesActual + 1) if ingresos_por_mes[m] > 0]
    if not con_ingresos:
        return {
            "anio": s.anio,
            "hayDatos": False,
            "mensaje": f"No hay boletas emitidas en {s.anio}: no hay datos para analizar.",
            "recomendaciones": ["Registre y cobre órdenes de trabajo para obtener un análisis del año."],
        }
    meses = list(range(con_ingresos[0], con_ingresos[-1] + 1))
    serie = [ingresos_por_mes[m] for m in meses]
    total = round(sum(serie), 2)
    total_ordenes = sum(ordenes_por_mes[m] for m in meses)

    crecimiento = crecimiento_intermensual(meses, serie)
    pico_mes = max(meses, key=lambda m: ingresos_por_mes[m])
    menor_mes = min(meses, key=lambda m: ingresos_por_mes[m])
    proyeccion = proyectar(s.anio, meses, serie)
    formato_top = formato_mas_rentable(s.formatos)

    resultado = {
        "anio": s.anio,
        "hayDatos": True,
        "mesesAnalizados": len(meses),
        "periodo": f"{MESES[meses[0] - 1]} a {MESES[meses[-1] - 1]} de {s.anio}",
        "serie": [{"mes": m, "nombre": MESES[m - 1], "ingresos": round(ingresos_por_mes[m], 2)} for m in meses],
        "totalIngresos": total,
        "totalOrdenes": total_ordenes,
        "promedioMensual": round(total / len(meses), 2),
        "ticketPromedio": round(total / total_ordenes, 2) if total_ordenes else None,
        "crecimiento": crecimiento,
        "crecimientoPromedio": crecimiento_compuesto(serie),
        "mesPico": {"mes": pico_mes, "nombre": MESES[pico_mes - 1], "ingresos": round(ingresos_por_mes[pico_mes], 2)},
        "mesMasBajo": {"mes": menor_mes, "nombre": MESES[menor_mes - 1], "ingresos": round(ingresos_por_mes[menor_mes], 2)},
        "formatoMasRentable": formato_top,
        "proyeccion": proyeccion,
    }
    resultado["recomendaciones"] = recomendaciones(resultado, s.formatos)
    return resultado


def crecimiento_intermensual(meses: List[int], serie: List[float]) -> List[dict]:
    """Tasa de crecimiento de cada mes respecto del anterior (%). None si el mes anterior fue 0."""
    filas = []
    for i in range(1, len(meses)):
        anterior, actual = serie[i - 1], serie[i]
        tasa = round((actual - anterior) / anterior * 100, 2) if anterior > 0 else None
        filas.append({"mes": meses[i], "nombre": MESES[meses[i] - 1], "ingresos": round(actual, 2), "tasa": tasa})
    return filas


def crecimiento_compuesto(serie: List[float]) -> Optional[float]:
    """Tasa de crecimiento mensual compuesta (%) entre el primer y el último mes con ingresos.
    Es más estable que el promedio simple de las tasas, que se distorsiona con un mes sin ventas (-100 %)."""
    con_ventas = [i for i, v in enumerate(serie) if v > 0]
    if len(con_ventas) < 2:
        return None
    primero, ultimo = con_ventas[0], con_ventas[-1]
    periodos = ultimo - primero
    tasa = (serie[ultimo] / serie[primero]) ** (1 / periodos) - 1
    return round(tasa * 100, 2)


def proyectar(anio: int, meses: List[int], serie: List[float]) -> dict:
    """Regresión lineal por mínimos cuadrados (statistics.linear_regression) de los ingresos mensuales."""
    if len(meses) < MINIMO_MESES_PROYECCION:
        return {
            "disponible": False,
            "mensaje": f"Se necesitan al menos {MINIMO_MESES_PROYECCION} meses con datos para proyectar "
                       f"(hay {len(meses)}).",
        }
    x = list(range(1, len(serie) + 1))
    try:
        pendiente, intercepto = linear_regression(x, serie)
    except StatisticsError as e:
        return {"disponible": False, "mensaje": f"No se pudo calcular la regresión: {e}"}

    media = fmean(serie)
    ss_tot = sum((y - media) ** 2 for y in serie)
    ss_res = sum((y - (pendiente * xi + intercepto)) ** 2 for xi, y in zip(x, serie))
    r2 = 1 - ss_res / ss_tot if ss_tot > 0 else 1.0

    futuros = []
    anio_p, mes_p = anio, meses[-1]
    for k in range(1, MESES_PARA_PROYECTAR + 1):
        mes_p += 1
        if mes_p > 12:
            mes_p, anio_p = 1, anio_p + 1
        estimado = max(0.0, pendiente * (len(serie) + k) + intercepto)
        futuros.append({"anio": anio_p, "mes": mes_p, "nombre": MESES[mes_p - 1], "ingresoEstimado": round(estimado, 2)})

    return {
        "disponible": True,
        "metodo": "Regresión lineal por mínimos cuadrados sobre los ingresos mensuales",
        "pendiente": round(pendiente, 2),
        "intercepto": round(intercepto, 2),
        "r2": round(r2, 3),
        "confiable": len(serie) >= 6 and r2 >= 0.5,
        "tendencia": tendencia(pendiente, r2, media),
        "meses": futuros,
    }


def tendencia(pendiente: float, r2: float, media: float) -> str:
    """Solo se habla de tendencia si la recta explica algo de la variación (R² >= 0.2) y la pendiente
    es de al menos el 1 % del ingreso mensual promedio; si no, los ingresos se consideran estables."""
    if r2 < 0.2 or abs(pendiente) < 0.01 * media:
        return "estable"
    return "creciente" if pendiente > 0 else "decreciente"


def formato_mas_rentable(formatos: List[IngresoFormato]) -> Optional[dict]:
    validos = [f for f in formatos if (f.ingresos or 0) > 0]
    if not validos:
        return None
    total = sum(f.ingresos for f in validos)
    top = max(validos, key=lambda f: f.ingresos)
    return {
        "formato": top.formato,
        "categoria": top.categoria,
        "ingresos": round(top.ingresos, 2),
        "ordenes": top.ordenes,
        "participacion": round(top.ingresos / total * 100, 2) if total else None,
    }


def recomendaciones(r: dict, formatos: List[IngresoFormato]) -> List[str]:
    """Recomendaciones ejecutivas automáticas a partir de reglas simples y explicables."""
    recs = []
    proy = r["proyeccion"]
    if proy.get("disponible"):
        if proy["tendencia"] == "creciente":
            recs.append(f"La tendencia de ingresos es creciente (+S/ {proy['pendiente']:,.2f} por mes en promedio): "
                        "prevea capacidad de laboratorio (técnicos y equipos) para los próximos meses.")
        elif proy["tendencia"] == "decreciente":
            recs.append(f"La tendencia de ingresos es decreciente ({proy['pendiente']:,.2f} S/ por mes): "
                        "conviene lanzar una campaña de captación o promociones en los formatos más rentables.")
        else:
            recs.append("Los ingresos no muestran una tendencia clara (se mantienen alrededor del promedio "
                        f"de S/ {r['promedioMensual']:,.2f} por mes): mantenga la capacidad actual y revise "
                        "los meses más bajos.")
        if not proy["confiable"]:
            recs.append(f"La proyección es solo referencial (R² = {proy['r2']}, {r['mesesAnalizados']} meses): "
                        "los ingresos varían mucho de un mes a otro.")
    else:
        recs.append(proy["mensaje"])

    top = r["formatoMasRentable"]
    if top:
        if top["participacion"] is not None and top["participacion"] >= 40:
            recs.append(f"{top['formato']} concentra el {top['participacion']}% de los ingresos: diversifique la oferta "
                        "para no depender de un solo formato.")
        else:
            recs.append(f"{top['formato']} es el formato más rentable (S/ {top['ingresos']:,.2f}): priorícelo en la "
                        "publicidad y en la cola del laboratorio.")
        rentables = sorted([f for f in formatos if (f.ingresos or 0) > 0], key=lambda f: f.ingresos)
        if len(rentables) >= 3:
            recs.append(f"{rentables[0].formato} es el formato con menos ingresos: evalúe su precio o promociónelo "
                        "junto con los formatos más solicitados.")

    if r["crecimientoPromedio"] is not None and r["crecimientoPromedio"] <= -1:
        recs.append("El crecimiento mensual compuesto es negativo: revise las órdenes pendientes de cobro "
                    "(estado Por Notificar) y el seguimiento a clientes.")
    recs.append(f"Mes pico: {r['mesPico']['nombre']} (S/ {r['mesPico']['ingresos']:,.2f}). Planifique personal y "
                "materiales con anticipación para meses de demanda similar.")
    return recs
