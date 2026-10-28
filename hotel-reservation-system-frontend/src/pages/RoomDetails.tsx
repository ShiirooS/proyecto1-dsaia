import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Header from '../components/Header'
import HeaderBanner from '../components/HeaderBanner'
import Button from '../components/Button'
import AvailabilityCalendar, { type DateRange } from '../components/calendar/AvailabilityCalendar'
import { nightsBetween } from '../components/calendar/calendarUtils'
import { getRoom } from '../api/rooms'
import { ApiError } from '../api/client'
import type { RoomDetail } from '../types/api'
import { formatPrice } from '../utils/format'

type RoomState = { status: 'loading' } | { status: 'not-found' } | { status: 'error' } | { status: 'ready'; room: RoomDetail }

export default function RoomDetails() {
  const { id = '' } = useParams()
  const [state, setState] = useState<RoomState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [range, setRange] = useState<DateRange>({})
  const [guests, setGuests] = useState(1)

  useEffect(() => {
    let cancelled = false
    setState({ status: 'loading' })
    setRange({})
    setGuests(1)
    getRoom(id)
      .then((room) => !cancelled && setState({ status: 'ready', room }))
      .catch((e: unknown) => {
        if (cancelled) return
        setState({ status: e instanceof ApiError && e.status === 404 ? 'not-found' : 'error' })
      })
    return () => {
      cancelled = true
    }
  }, [id, attempt])

  return (
    <div>
      <Header />
      <HeaderBanner />
      <main className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
        {state.status === 'loading' && <p role="status" className="text-center text-gray-600">Cargando habitación...</p>}
        {state.status === 'not-found' && (
          <div className="text-center">
            <h1 className="text-2xl font-semibold text-gray-900">Habitación no encontrada</h1>
            <Link to="/products" className="mt-4 inline-block text-orange-600 underline hover:text-orange-700">Volver al listado de habitaciones</Link>
          </div>
        )}
        {state.status === 'error' && (
          <div role="alert" className="text-center text-red-700">
            <p>No pudimos cargar la habitación.</p>
            <button type="button" onClick={() => setAttempt((n) => n + 1)} className="mt-3 rounded-lg bg-orange-600 px-5 py-2 text-white hover:bg-orange-700">
              Reintentar
            </button>
          </div>
        )}
        {state.status === 'ready' && (
          <RoomContent room={state.room} range={range} onRangeChange={setRange} guests={guests} onGuestsChange={setGuests} />
        )}
      </main>
    </div>
  )
}

interface RoomContentProps {
  room: RoomDetail
  range: DateRange
  onRangeChange: (range: DateRange) => void
  guests: number
  onGuestsChange: (guests: number) => void
}

function RoomContent({ room, range, onRangeChange, guests, onGuestsChange }: RoomContentProps) {
  const { checkIn, checkOut } = range
  const nights = checkIn && checkOut ? nightsBetween(checkIn, checkOut) : 0
  const total = nights * room.pricePerNight
  const checkoutPath =
    checkIn && checkOut ? `/rooms/${room.id}/checkout?checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}` : null

  return (
    <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
      <div className="space-y-6">
        {room.imageUrl ? (
          <img src={room.imageUrl} alt={`Habitación ${room.roomNumber} - ${room.hotel.name}`} className="h-80 w-full rounded-lg object-cover shadow-lg" />
        ) : (
          <div role="img" aria-label="Sin imagen" className="flex h-80 w-full items-center justify-center rounded-lg bg-gray-100 text-gray-400">Sin imagen</div>
        )}
        <div>
          <h1 className="text-3xl font-bold">{room.type} · Habitación {room.roomNumber}</h1>
          <p className="mt-1 text-gray-600">{room.hotel.name} — {room.hotel.location}</p>
          <p className="mt-2 text-2xl font-semibold text-orange-600">{formatPrice(room.pricePerNight)} / noche</p>
          <p className="mt-2 text-gray-700">Capacidad: {room.capacity} {room.capacity === 1 ? 'huésped' : 'huéspedes'}</p>
          {room.description && <p className="mt-4 text-gray-700">{room.description}</p>}
        </div>
      </div>

      <section aria-labelledby="reserva-titulo" className="space-y-6">
        <h2 id="reserva-titulo" className="text-xl font-semibold">Elige tus fechas</h2>
        <AvailabilityCalendar roomId={room.id} value={range} onChange={onRangeChange} />

        <div>
          <label htmlFor="guests" className="block text-sm font-medium text-gray-700">Huéspedes</label>
          <select
            id="guests"
            value={guests}
            onChange={(e) => onGuestsChange(Number(e.target.value))}
            className="mt-1 block w-full rounded-lg border border-gray-300 p-2 focus:border-orange-500 focus:outline-none focus:ring focus:ring-orange-300"
          >
            {Array.from({ length: room.capacity }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>{n}</option>
            ))}
          </select>
        </div>

        <div aria-live="polite" className="rounded-lg bg-gray-50 p-4 text-gray-800">
          {nights > 0 ? (
            <p>
              <span>{nights} {nights === 1 ? 'noche' : 'noches'} × {formatPrice(room.pricePerNight)} = </span>
              <strong data-testid="total">{formatPrice(total)}</strong>
            </p>
          ) : (
            <p>Selecciona la fecha de entrada y de salida.</p>
          )}
        </div>

        {checkoutPath ? (
          <Button to={checkoutPath} content="Reservar" className="block w-full py-3 text-center" />
        ) : (
          <button type="button" disabled className="block w-full cursor-not-allowed rounded-lg bg-gray-300 px-5 py-3 text-sm font-medium text-gray-500">
            Reservar
          </button>
        )}
      </section>
    </div>
  )
}
