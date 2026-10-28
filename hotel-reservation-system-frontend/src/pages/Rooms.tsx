import { useEffect, useState } from 'react'
import CardRoom from '../components/CardRoom'
import SideBarFilter from '../components/SideBarFilter'
import Header from '../components/Header'
import HeaderBanner from '../components/HeaderBanner'
import { getRooms } from '../api/rooms'
import type { RoomSummary } from '../types/api'

type RoomsState = { status: 'loading' } | { status: 'error' } | { status: 'ready'; rooms: RoomSummary[] }

export default function Product() {
  const [state, setState] = useState<RoomsState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    setState({ status: 'loading' })
    getRooms()
      .then((rooms) => !cancelled && setState({ status: 'ready', rooms }))
      .catch(() => !cancelled && setState({ status: 'error' }))
    return () => {
      cancelled = true
    }
  }, [attempt])

  return (
    <div>
        <Header />
        <HeaderBanner/>

        <main className='grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 lg:gap-8 gap-4 max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-2'>
          <aside className="sticky top-0 w-full md:w-1/1 p-6 bg-white shadow-sm h-screen overflow-y-auto rounded-lg">
              <SideBarFilter/>
          </aside>
          <div className="h rounded-lg bg-white shadow-sm  lg:col-span-2">
            <section className="container mx-auto max-w-screen-lg p-4" aria-label="Habitaciones">
              {state.status === 'loading' && <p role="status" className="text-center text-gray-600">Cargando habitaciones...</p>}
              {state.status === 'error' && (
                <div role="alert" className="text-center text-red-700">
                  <p>No pudimos cargar las habitaciones.</p>
                  <button type="button" onClick={() => setAttempt((n) => n + 1)} className="mt-3 rounded-lg bg-orange-600 px-5 py-2 text-white hover:bg-orange-700">
                    Reintentar
                  </button>
                </div>
              )}
              {state.status === 'ready' && state.rooms.length === 0 && (
                <p className="text-center text-gray-600">No hay habitaciones disponibles por ahora.</p>
              )}
              {state.status === 'ready' && state.rooms.length > 0 && (
                <ul className="grid grid-cols-1 sm:grid-cols-2 gap-10">
                  {state.rooms.map((room) => (
                    <li key={room.id}><CardRoom room={room} /></li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </main>
    </div>
  )
}
