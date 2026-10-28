import { useEffect, useState } from 'react'
import axios from 'axios'
import { getAvailability, type IsoDate } from '../../services/roomService'
import { addMonths, lastDayOfMonth } from './calendarUtils'

interface Loaded {
  rangeKey: string
  occupied: ReadonlySet<IsoDate>
  today: IsoDate
  maxNights: number
}

/**
 * Consulta la disponibilidad de los dos meses visibles en cada cambio de vista. Las noches
 * ocupadas se acumulan entre consultas (reemplazando solo el rango consultado) para validar
 * estancias que cruzan meses ya visitados.
 */
export function useAvailability(roomId: number | string, viewMonth: IsoDate) {
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  const from = viewMonth
  const to = lastDayOfMonth(addMonths(viewMonth, 1))
  const rangeKey = `${from}|${to}`

  useEffect(() => {
    let cancelled = false
    setError(null)
    getAvailability(roomId, from, to)
      .then((res) => {
        if (cancelled) return
        setLoaded((prev) => {
          const kept = [...(prev?.occupied ?? [])].filter((night) => night < from || night > to)
          return {
            rangeKey: `${from}|${to}`,
            occupied: new Set([...kept, ...res.occupiedDates]),
            today: res.today,
            maxNights: res.maxNights,
          }
        })
      })
      .catch((e: unknown) => {
        if (cancelled) return
        setError(
          axios.isAxiosError(e) && e.response?.status === 404
            ? 'Habitación no encontrada'
            : 'No se pudo cargar la disponibilidad',
        )
      })
    return () => {
      cancelled = true
    }
  }, [roomId, from, to, attempt])

  return {
    today: loaded?.today ?? null,
    maxNights: loaded?.maxNights ?? 30,
    occupied: loaded?.occupied ?? new Set<IsoDate>(),
    loading: error === null && loaded?.rangeKey !== rangeKey,
    error,
    retry: () => setAttempt((n) => n + 1),
  }
}
