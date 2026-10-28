# ADR-005 — Disponibilidad derivada de las reservas y control de solapamiento

- **Estado**: Aceptado (2026-10-28)
- **HU**: HU-02 (lo usan HU-01 al reservar y HU-05 al cancelar)

## Contexto

`Room.availbility` es un booleano manual: no dice qué días están libres, nadie lo actualiza y no coincide con `RoomDTO.availability`. HU-02 pide un calendario con días libres y ocupados "sin intentos fallidos". HU-05 exige que cancelar libere las fechas. Dos clientes pueden intentar reservar las mismas noches a la vez.

## Decisión

1. **No se almacena disponibilidad.** Una noche `d` de la habitación `r` está ocupada si existe una reserva de `r` con `status = CONFIRMED` y `startDate <= d < endDate` (intervalo semiabierto: el día de check-out queda libre). En la entidad las fechas siguen llamándose `startDate`/`endDate` (ahora `LocalDate`); el JSON de los endpoints nuevos las expone como `checkIn`/`checkOut`.
2. `Room.availbility` (JSON `availability`) **no se renombra** para no romper el frontend de `developer`; se redefine como "habitación activa / a la venta". Una habitación inactiva no se reserva ni expone disponibilidad (404) y la vista pública la filtra con `activeOnly=true`.
3. **Solapamiento** (JPQL, portable):
   ```
   select count(r) > 0 from Reservation r
   where r.room.id = :roomId and r.status = CONFIRMED
     and r.startDate < :checkOut and r.endDate > :checkIn
   ```
4. **Concurrencia**: al crear una reserva, dentro de la transacción y **antes** de comprobar el solapamiento, se bloquea la fila de la habitación con `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`RoomRepository.findByIdForUpdate`, que se traduce en `SELECT ... FOR UPDATE` en H2 y en Postgres). Así las reservas de una misma habitación se serializan; las de habitaciones distintas no se bloquean entre sí.
5. **Días ocupados** para el calendario: se leen las reservas `CONFIRMED` que intersectan `[from, to + 1)` y se expanden a fechas en Java (`OccupancyCalculator`). Rango máximo 366 días.
6. Cancelar = `status CANCELLED`. La noche se libera automáticamente porque solo cuentan las `CONFIRMED`.

## Alternativas consideradas

| Alternativa | Por qué no |
|-------------|------------|
| Mantener el flag `availbility` | No modela fechas; se desincroniza. |
| Restricción `EXCLUDE USING gist (daterange)` | Es la garantía más fuerte, pero solo existe en Postgres y rompe ADR-002 (H2). |
| Tabla `room_night(room_id, night)` con clave única | Robusta en cualquier BD, pero duplica datos y complica cancelar y consultar. Reservada como plan B si el bloqueo da problemas. |
| Bloqueo optimista (`@Version` en `Room`) | Obliga a reintentar en el cliente o en el servidor; más código para el mismo resultado. |
| Aislamiento `SERIALIZABLE` | Comportamiento distinto en H2 y Postgres; difícil de probar. |

## Consecuencias

- (+) El calendario y la validación del servidor usan exactamente la misma regla; el 409 solo aparece en condiciones de carrera reales.
- (+) Cancelar libera las fechas sin código adicional.
- (−) El bloqueo por habitación limita el rendimiento de escrituras concurrentes sobre la **misma** habitación (irrelevante a esta escala).
- (−) La consulta de solapamiento necesita un índice en `reservations(room_id, status, start_date, end_date)` cuando crezcan los datos (se anota; `ddl-auto` no lo crea por sí solo — usar `@Table(indexes = ...)`).
- Prueba obligatoria: test MockMvc de 409 con rango solapado. Recomendada: test de concurrencia con dos hilos y `CountDownLatch` que espera exactamente un 201 y un 409.
