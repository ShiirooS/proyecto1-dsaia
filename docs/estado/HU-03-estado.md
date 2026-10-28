# HU-03 — Gráficos de previsión de demanda (admin): registro de estado

| Campo | Valor |
|-------|-------|
| Estado | **Hecha · en PR** |
| Issue | #4 |
| Pull request | (se completa al abrir el PR) |
| Rama | `feature/HU-03-prevision-demanda` (sale de `feature/HU-05-mis-reservas-cancelacion`: las dos tocan `DevDataSeeder`) |
| Especificación | [../user-stories/HU-03.md](../user-stories/HU-03.md) |

## Qué se implementó
- El avance parcial anterior (`DemandForecastCalculator`, `PricingRules`, `nightsPerMonth`) nunca se subió; se reimplementó desde ADR-004.
- `domain/DemandForecastCalculator`: OLS de tendencia + estacionalidad mensual con la solución cerrada de ADR-004 §3; redondeo a 4 decimales HALF_UP antes de aplicar reglas.
- `domain/PricingRules`: umbrales de precio (+15/+8/0/−10) e inventario (objetivo 80 %) en `BigDecimal` exacto.
- `OccupancyCalculator.nightsPerMonth`.
- `GET /api/analytics/demand-forecast?horizon&hotelId` (solo ADMIN): 400 `INVALID_PARAMETER`, 404 `HOTEL_NOT_FOUND`, 422 `INSUFFICIENT_HISTORY`.
- Seed `dev`: 24 meses de histórico (`Random(42)`) con picos en julio, agosto y diciembre y +0.5 pp/mes de tendencia, sin solapes con las reservas de demo ni con las próximas.
- Frontend: `/admin/dashboard` (pestaña dashboard) con KPIs reales, `ForecastChart` (real, tendencia, previsión discontinua y "ya reservado"), `SeasonalityChart`, `ForecastTable` y selectores de horizonte (3/6/12) y hotel. Estados de carga, histórico insuficiente y error con "Reintentar".
- `recharts@2.15.4` fijado; el panel de admin se carga con `React.lazy`, así el bundle de huéspedes no lo descarga (314 kB frente a 364 kB antes de HU-03).

## Desviaciones del brief
- Solo cuentan las reservas de habitaciones **activas**, para que numerador y denominador (`R` = habitaciones activas) tengan el mismo alcance.
- `GlobalExceptionHandler` también mapea `HandlerMethodValidationException` a `INVALID_PARAMETER` (estaba en el contrato §0.2 y faltaba).

## Pruebas
- Backend: `DemandForecastCalculatorTest` (8: las 12 filas de ADR-004 §5.1, anti-regresión b = 0.007513 de la descomposición ingenua, §5.2), `PricingRulesTest` (13, §5.3 y redondeo de precio), `OccupancyCalculatorTest` (+2, §5.4), `AnalyticsControllerTest` (6, serie construida con resultado exacto: habitación inactiva y reserva cancelada no cuentan), `DevDataSeederTest` (3: CA-03.9, sin solapes en todo el seed, reservas de demo).
- Frontend: `Dashboard.test.tsx` (5), `SeasonalityChart.test.tsx` (2), `forecastUtils.test.ts` (3).
- QA manual con `admin` en `dev`: picos en jul–ago y dic, pendiente recuperada ≈ +0.5 pp/mes, horizonte 12 + hotel → `horizon=12&hotelId=2` y 12 filas; umbrales comprobados a mano (85.8 % → +15 %, $145 × 1.15 = $167, 5 habitaciones).

## Pendiente
- Nada de la HU. Merge del PR (después del de HU-05) y cierre manual del issue #4.
