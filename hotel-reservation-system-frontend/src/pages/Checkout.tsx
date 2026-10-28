import { Link, useParams, useSearchParams } from 'react-router-dom'
import Header from '../components/Header'
import { nightsBetween } from '../components/calendar/calendarUtils'

/** Placeholder de HU-02: solo muestra el resumen recibido por query; HU-01 crea la reserva. */
export default function Checkout() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const checkIn = params.get('checkIn')
  const checkOut = params.get('checkOut')
  const guests = params.get('guests')

  return (
    <div>
      <Header />
      <main className="mx-auto max-w-2xl px-4 py-12">
        <h1 className="text-3xl font-bold">Resumen de tu reserva</h1>
        {checkIn && checkOut ? (
          <dl className="mt-6 space-y-2 text-gray-800">
            <div><dt className="inline font-medium">Habitación: </dt><dd className="inline">{id}</dd></div>
            <div><dt className="inline font-medium">Entrada: </dt><dd className="inline">{checkIn}</dd></div>
            <div><dt className="inline font-medium">Salida: </dt><dd className="inline">{checkOut}</dd></div>
            <div><dt className="inline font-medium">Noches: </dt><dd className="inline">{nightsBetween(checkIn, checkOut)}</dd></div>
            <div><dt className="inline font-medium">Huéspedes: </dt><dd className="inline">{guests ?? '1'}</dd></div>
          </dl>
        ) : (
          <p role="alert" className="mt-6 text-red-700">Faltan las fechas de la reserva.</p>
        )}
        <Link to={`/rooms/${id}`} className="mt-8 inline-block text-orange-600 underline hover:text-orange-700">Volver a la habitación</Link>
      </main>
    </div>
  )
}
