# HU-03 — Gráficos de previsión de demanda (admin): registro de estado

| Campo | Valor |
|-------|-------|
| Estado | **Parcial · NO mergeada** |
| Issue | #4 |
| Pull request | — |
| Rama | `feature/HU-03-prevision-demanda (local, sin push)` |
| Especificación | [../user-stories/HU-03.md](../user-stories/HU-03.md) |

## Qué se implementó
- Hecho: `DemandForecastCalculator` (OLS tendencia + estacionalidad mensual, ADR-004), `PricingRules`, `OccupancyCalculator.nightsPerMonth`.

## Pruebas
- DemandForecastCalculatorTest (vectores del ADR-004), PricingRulesTest, OccupancyCalculatorTest: en verde.

## Pendiente
- `DemandForecastDTO`, `AnalyticsService(Imp)` y `AnalyticsController` (`GET /api/analytics/demand-forecast?horizon&hotelId`, horizon 1-12).
- Seed de 24 meses de histórico.
- Frontend con recharts: ForecastChart, SeasonalityChart, ForecastTable, Dashboard.
- Rebase sobre develop, PR (Closes #4) y merge.
