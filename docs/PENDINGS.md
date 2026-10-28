# Pendientes

Actualizado: 2026-10-28 (tras mergear HU-00 a HU-05 en `develop`).

## Desarrollo
Las seis historias están mergeadas (PR #8, #11, #9, #13, #16, #15) y los issues #1 a #6 cerrados. `develop` pasa 106 tests de backend, 96 de frontend, `tsc -b` y el build.

Deuda técnica menor (ninguna bloquea las HU):
1. **HU-01**: mapear `PessimisticLockingFailureException` a 409/503.
2. **HU-01**: unique constraint en `payment_method` (usuario, marca, últimos 4, vencimiento).
3. **HU-01**: `ReservationConcurrencyTest` (dos reservas simultáneas de las mismas noches).
4. **HU-01**: `savedPaymentMethodId` siempre presente en la respuesta (null si no se guardó).

## Entorno local
- El backend necesita JDK 17 (como CI): con JDK 27 Lombok 1.18.34 no genera getters y la compilación falla.

## Evidencia (se deja para el final)
- Consolidar `ai-log/raw.md` en el AI Development Log (actividad, herramienta, qué se pidió, resultado, decisión humana).
- Ejemplos de respuestas incorrectas de la IA, cambios rechazados y decisiones humanas.
- Material para la presentación.

## Cierre
- Los merges a `develop` no cierran issues solos: cerrarlos a mano.
- Cualquier fix de un check fallido va en una rama `fix/HU-XX-descripcion`.
