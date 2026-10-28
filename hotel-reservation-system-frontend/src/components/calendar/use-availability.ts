import { useEffect, useState } from 'react'
import { getAvailability } from '../../api/rooms'
import { ApiError } from '../../api/client'
import type { IsoDate } from '../../types/api'
import { addMonths, lastDayOfMonth } from './calendarUtils'

interface Cache {
  roomId: number | string
  loadedMonths: ReadonlySet<IsoDate>
  occupied: ReadonlySet<IsoDate>
  today: IsoDate | null
  maxNights: number
}

const emptyCache = (roomId: number | string): Cache => ({
  roomId,
  loadedMonths: new Set(),
  occupied: new Set(),
  today: null,
  maxNights: 30,
})

/**
 * Carga la disponibilidad de los dos meses visibles y la acumula por mes, de modo que
 * navegar a un mes ya visto no vuelve a pedir datos. La cache queda atada a `roomId`
 * para no mezclar noches de habitaciones distintas.
 */
export function useAvailability(roomId: number | string, viewMonth: IsoDate) {
  const [stored, setStored] = useState<Cache>(() => emptyCache(roomId))
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  const cache = stored.roomId === roomId ? stored : emptyCache(roomId)
  const missingMonths = [viewMonth, addMonths(viewMonth, 1)].filter((m) => !cache.loadedMonths.has(m))
  const missingKey = missingMonths.join(',')

  useEffect(() => {
    if (!missingKey) return
    const months = missingKey.split(',')
    let cancelled = false
    setError(null)
    getAvailability(roomId, months[0], lastDayOfMonth(months[months.length - 1]))
      .then((res) => {
        if (cancelled) return
        setStored((prev) => {
          const base = prev.roomId === roomId ? prev : emptyCache(roomId)
          return {
            roomId,
            loadedMonths: new Set([...base.loadedMonths, ...months]),
            occupied: new Set([...base.occupied, ...res.occupiedDates]),
            today: res.today,
            maxNights: res.maxNights,
          }
        })
      })
      .catch((e: unknown) => {
        if (cancelled) return
        setError(
          e instanceof ApiError && e.status === 404
            ? 'Habitación no encontrada.'
            : 'No pudimos cargar la disponibilidad.',
        )
      })
    return () => {
      cancelled = true
    }
  }, [roomId, missingKey, attempt])

  return {
    today: cache.today,
    maxNights: cache.maxNights,
    occupied: cache.occupied,
    loading: missingMonths.length > 0 && error === null,
    error: missingMonths.length > 0 ? error : null,
    retry: () => setAttempt((n) => n + 1),
  }
}
