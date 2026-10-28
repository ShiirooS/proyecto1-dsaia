# ADR-004 — Algoritmo de previsión de demanda (HU-03)

- **Estado**: Aceptado (2026-10-28). Este documento es **normativo**: el código y los tests de `DemandForecastCalculator` y `PricingRules` deben reproducir exactamente los vectores de la §5.
- **HU**: HU-03 · Endpoint: API-CONTRACT §6.1

## 1. Contexto

El administrador quiere "gráficos de previsión de la demanda basados en el histórico, para ajustar los precios y el inventario". Restricciones: el algoritmo debe ser determinista y verificable a mano (lo revisa un humano y se presenta en clase), no puede usar SQL específico de Postgres (ADR-002) y el histórico disponible es de 24 meses (seed del perfil `dev`).

## 2. Decisión

**Modelo de tendencia lineal + estacionalidad mensual ajustado por mínimos cuadrados ordinarios (OLS)**:

  y_t = b·t + c_p(t)        (equivalente a regresión con intercepto, `t` y 11 variables ficticias de mes)

Con exactamente dos ciclos de 12 meses, el ajuste OLS tiene solución cerrada, así que no hace falta álgebra matricial ni librerías.

> **Desviación consciente respecto del brief.** El brief proponía una "descomposición clásica": índices estacionales a partir de las medias por mes, desestacionalizar y después regresión lineal. Con solo 24 puntos, las medias por mes absorben parte de la tendencia y la pendiente sale **subestimada un 25 %**. En el caso de prueba de la §5.1, la pendiente real es 0.0100 y la descomposición ingenua da 0.0075; el pronóstico de enero baja de 0.6900 a 0.6039. El modelo OLS conjunto produce los mismos conceptos (tendencia + índices mensuales) sin ese sesgo.

## 3. Algoritmo (paso a paso)

Entradas: `clock`, `hotelId` opcional, `H` (horizonte 1–12, por defecto 6).

1. **Ventana.** `M_25` = mes actual (`YearMonth.now(clock)`). `M_1..M_24` = los 24 meses completos anteriores (`M_t = M_25.minusMonths(25 − t)`).
2. **Inventario.** `R` = número de habitaciones `active` en el alcance. Si `R = 0` → `INSUFFICIENT_HISTORY`. *Limitación aceptada*: se supone `R` constante durante todo el histórico.
3. **Ocupación mensual** (`OccupancyCalculator`, en Java). `O_t` = Σ, sobre las reservas `CONFIRMED` del alcance, de los días de `[checkIn, checkOut) ∩ [primer día de M_t, primer día de M_t+1)`. `Cap_t = R × díasDelMes(M_t)`. `y_t = min(1, O_t / Cap_t)` (en `double`). Si `Σ O_t = 0` → `INSUFFICIENT_HISTORY`.
4. **Posición en el ciclo.** `p(t) = ((t − 1) mod 12) + 1`. El mes calendario de la posición `p` es el de `M_p`.
5. **Pendiente.**  `b = (1/144) · Σ_{t=13..24} (y_t − y_{t−12})`  (cambio interanual medio dividido entre 12).
6. **Componente por posición.**  `c_p = (y_p + y_{p+12}) / 2 − b·(p + 6)`,  p = 1..12.
7. **Intercepto e índices estacionales.**  `a = (1/12) · Σ c_p` ;  `S_p = c_p − a`  (se cumple `Σ S_p = 0`). Se publican por mes calendario (`seasonalIndices[].month` = 1..12, enero = 1).
8. **Tendencia histórica** (para el gráfico): `trendRate_t = a + b·t`.
9. **Pronóstico.** Para `h = 1..H`, `t = 24 + h`: `ŷ_t = clamp(a + b·t + S_p(t), 0, 1)`. Se redondea a 4 decimales con `BigDecimal` `HALF_UP` → `occ`. **Todas las reglas siguientes usan `occ` ya redondeado**, en `BigDecimal`, para evitar errores de coma flotante en los umbrales.
10. **Noches previstas.** `forecastRoomNights = round(occ × R × díasDelMes, HALF_UP)` (entero).
11. **Precio** (`PricingRules`):

    | `occ` | `priceAdjustmentPct` |
    |-------|---------------------|
    | `> 0.85` | +15 |
    | `0.70 ≤ occ ≤ 0.85` | +8 |
    | `0.40 ≤ occ < 0.70` | 0 |
    | `< 0.40` | −10 |

    `suggestedPricePerNight = round(P̄ × (100 + pct) / 100, 2, HALF_UP)`, donde `P̄` es la media de `pricePerNight` de las habitaciones activas del alcance (también redondeada a 2 decimales).
12. **Inventario** (`PricingRules`), con ocupación objetivo `0.80`:

    | Condición | `inventoryAction` | `recommendedActiveRooms` |
    |-----------|-------------------|--------------------------|
    | `occ > 0.85` | `INCREASE` | `ceil(R × occ / 0.80)` |
    | `occ < 0.40` | `REDUCE` | `max(1, ceil(R × occ / 0.80))` |
    | resto | `KEEP` | `R` |

13. **Reservado a futuro** ("on the books"). `bookedRoomNights` = paso 3 aplicado al mes `M_{24+h}` (solo `CONFIRMED`); `bookedOccupancyRate = round(booked / (R × días), 4)`. Es informativo y **no** altera el pronóstico.
14. Todas las tasas de salida se redondean a 4 decimales; `slope`, `intercept` e índices también.

### Justificación de las fórmulas 5–6

Para una `b` fija, el `c_p` que minimiza el error cuadrático es la media de `y − b·t` en sus dos observaciones (`t = p` y `t = p + 12`, cuya media de `t` es `p + 6`). Con `δ_p = (y_{p+12} − y_p)/2`, los residuos quedan en `±(δ_p − 6b)`. Entonces `SSE = 2·Σ(δ_p − 6b)²`, que se minimiza en `b = Σδ_p / 72 = Σ(y_{p+12} − y_p) / 144`.

## 4. Alternativas consideradas

| Alternativa | Por qué no |
|-------------|------------|
| Descomposición clásica ingenua (medias por mes → desestacionalizar → regresión) | Pendiente sesgada (−25 % en §5.1). |
| Descomposición con media móvil centrada 2×12 | Con 24 puntos se pierden 12; queda una sola observación por mes. |
| Holt-Winters | Requiere elegir α, β y γ, inicialización y más ciclos; no se verifica a mano. |
| Media móvil simple | Ignora la estacionalidad, que es lo que el administrador necesita ver. |
| Librería de ML/series temporales | Dependencia pesada, no determinista o difícil de explicar en la presentación. |

## 5. Casos de prueba normativos

Tolerancia de comparación en `double`: `1e-9` antes de redondear; las salidas redondeadas se comparan exactas.

### 5.1 Caso principal (reloj fijo `2027-01-15T10:00`, `R = 10`, `P̄ = 100.00`, `H = 12`)

Entrada `y_t` (ventana `2025-01..2026-12`; el año 2 = el año 1 + 0.12):

| Mes | Ene | Feb | Mar | Abr | May | Jun | Jul | Ago | Sep | Oct | Nov | Dic |
|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|
| 2025 (t = 1..12) | 0.45 | 0.45 | 0.55 | 0.55 | 0.65 | 0.75 | 0.85 | 0.85 | 0.65 | 0.55 | 0.45 | 0.45 |
| 2026 (t = 13..24) | 0.57 | 0.57 | 0.67 | 0.67 | 0.77 | 0.87 | 0.97 | 0.97 | 0.77 | 0.67 | 0.57 | 0.57 |

Parámetros esperados: `slope b = 0.0100`, `intercept a = 0.5350`, `trendRate_1 = 0.5450`, `trendRate_24 = 0.7750`.

| Mes | Ene | Feb | Mar | Abr | May | Jun | Jul | Ago | Sep | Oct | Nov | Dic |
|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|
| `S` | −0.0950 | −0.1050 | −0.0150 | −0.0250 | 0.0650 | 0.1550 | 0.2450 | 0.2350 | 0.0250 | −0.0850 | −0.1950 | −0.2050 |

Pronóstico esperado:

| Mes | t | `a + b·t + S` | `occ` | Noches | % precio | Precio | Acción | Habitaciones |
|-----|---|---------------|-------|--------|----------|--------|--------|--------------|
| 2027-01 | 25 | 0.69 | 0.6900 | 214 | 0 | 100.00 | KEEP | 10 |
| 2027-02 | 26 | 0.69 | 0.6900 | 193 | 0 | 100.00 | KEEP | 10 |
| 2027-03 | 27 | 0.79 | 0.7900 | 245 | 8 | 108.00 | KEEP | 10 |
| 2027-04 | 28 | 0.79 | 0.7900 | 237 | 8 | 108.00 | KEEP | 10 |
| 2027-05 | 29 | 0.89 | 0.8900 | 276 | 15 | 115.00 | INCREASE | 12 |
| 2027-06 | 30 | 0.99 | 0.9900 | 297 | 15 | 115.00 | INCREASE | 13 |
| 2027-07 | 31 | **1.09** | **1.0000** (clamp) | 310 | 15 | 115.00 | INCREASE | 13 |
| 2027-08 | 32 | **1.09** | **1.0000** (clamp) | 310 | 15 | 115.00 | INCREASE | 13 |
| 2027-09 | 33 | 0.89 | 0.8900 | 267 | 15 | 115.00 | INCREASE | 12 |
| 2027-10 | 34 | 0.79 | 0.7900 | 245 | 8 | 108.00 | KEEP | 10 |
| 2027-11 | 35 | 0.69 | 0.6900 | 207 | 0 | 100.00 | KEEP | 10 |
| 2027-12 | 36 | 0.69 | 0.6900 | 214 | 0 | 100.00 | KEEP | 10 |

Con `H = 6` (valor por defecto) se devuelven solo las 6 primeras filas.

**Test anti-regresión**: este mismo caso con la descomposición ingenua da `b = 0.007513` y `ŷ_25 = 0.6039`. El test debe afirmar `b = 0.0100` y `occ(2027-01) = 0.6900`.

### 5.2 Casos adicionales

| Caso | Entrada | Esperado |
|------|---------|----------|
| Constante | `y_t = 0.50` para todo t | `b = 0`, todos los `S = 0`, `a = 0.50`, pronóstico `0.5000` en todos los meses |
| Lineal pura | `y_t = 0.20 + 0.01·t` | `b = 0.0100`, todos los `S = 0`, `a = 0.20`, `occ(t=25) = 0.4500` |
| Sin ocupación | `y_t = 0` para todo t | `INSUFFICIENT_HISTORY` |
| Sin inventario | `R = 0` | `INSUFFICIENT_HISTORY` |
| Clamp inferior | Tendencia negativa que da `ŷ < 0` | `occ = 0.0000`, `REDUCE`, `recommendedActiveRooms = 1` |

### 5.3 Umbrales de `PricingRules` (test parametrizado, `R = 10`)

| `occ` | % | Acción | Habitaciones |
|-------|---|--------|--------------|
| 0.3000 | −10 | REDUCE | 4 |
| 0.3999 | −10 | REDUCE | 5 |
| 0.4000 | 0 | KEEP | 10 |
| 0.6999 | 0 | KEEP | 10 |
| 0.7000 | 8 | KEEP | 10 |
| 0.8500 | 8 | KEEP | 10 |
| 0.8501 | 15 | INCREASE | 11 |
| 0.8800 | 15 | INCREASE | 11 (frontera exacta: 10 × 0.88 / 0.80 = 11.00 → `ceil` no debe subir a 12) |

### 5.4 `OccupancyCalculator`

| Reserva | Esperado |
|---------|----------|
| `2026-01-30 → 2026-02-02` | 2 noches en 2026-01 (30 y 31), 1 en 2026-02 |
| `2026-03-10 → 2026-03-10` (no válida) | ignorada (0 noches) |
| `2024-02-28 → 2024-03-01` (año bisiesto) | 2 noches en 2024-02 (28 y 29) |
| Reserva `CANCELLED` | 0 noches |

## 6. Consecuencias

- (+) Determinista, sin dependencias y verificable con una hoja de cálculo; los vectores sirven de oráculo para los tests.
- (+) Separa el cálculo puro (`DemandForecastCalculator`, `PricingRules`, `OccupancyCalculator`) de la carga de datos (`AnalyticsServiceImp`).
- (−) Exige exactamente 24 meses; los meses sin actividad cuentan como 0 (un hotel recién abierto obtiene una tendencia exagerada).
- (−) Supone inventario constante y no modela eventos puntuales (feriados móviles, congresos).
- (−) Las sugerencias no se aplican solas: no hay endpoint para cambiar precios ni activar habitaciones (fuera de alcance).
