import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Header from '../components/Layouts/Header';
import HeaderBanner from '../components/Layouts/HeaderBanner';
import ReservationCard from '../components/Bookings/ReservationCard';
import CancelReservationModal from '../components/Modals/CancelReservationModal';
import { BOOKING_TABS, groupByTab, type BookingTab } from '../components/Bookings/bookingUtils';
import { listMyReservations, type MyReservation } from '../services/reservationService';

const EMPTY_TAB_MESSAGES: Record<BookingTab, string> = {
  upcoming: 'No tienes reservas próximas.',
  past: 'Todavía no tienes estancias pasadas.',
  cancelled: 'No tienes reservas canceladas.',
};

/** HU-05: "Mis reservas" with upcoming, past and cancelled stays, and self-service cancellation. */
export default function Bookings() {
  const [reservations, setReservations] = useState<MyReservation[] | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [activeTab, setActiveTab] = useState<BookingTab>('upcoming');
  const [toCancel, setToCancel] = useState<MyReservation | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const tabRefs = useRef<Record<BookingTab, HTMLButtonElement | null>>({ upcoming: null, past: null, cancelled: null });

  const reload = useCallback(async () => {
    try {
      setReservations(await listMyReservations());
      setLoadFailed(false);
    } catch {
      setLoadFailed(true);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const handleCancelled = (updated: MyReservation) => {
    setToCancel(null);
    setReservations((current) => current?.map((r) => (r.id === updated.id ? updated : r)) ?? null);
    setActiveTab('cancelled');
    const refunded = updated.payment?.refundedAmount ?? 0;
    setNotice(
      refunded > 0
        ? `Reserva n.º ${updated.id} cancelada. Recibirás un reembolso de $${refunded}.`
        : `Reserva n.º ${updated.id} cancelada sin reembolso.`,
    );
  };

  const handleTabKeyDown = (event: React.KeyboardEvent, index: number) => {
    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
    const step = event.key === 'ArrowRight' ? 1 : -1;
    const next = BOOKING_TABS[(index + step + BOOKING_TABS.length) % BOOKING_TABS.length].id;
    setActiveTab(next);
    tabRefs.current[next]?.focus();
  };

  const groups = reservations ? groupByTab(reservations) : null;
  const visible = groups ? groups[activeTab] : [];

  return (
    <div>
      <Header />
      <HeaderBanner />
      <main className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <h1 className="mb-6 text-2xl font-bold text-gray-900">Mis reservas</h1>

        {notice && (
          <p role="status" className="mb-6 rounded-md bg-green-50 p-3 text-green-800">
            {notice}
          </p>
        )}

        {loadFailed ? (
          <p role="alert" className="text-red-700">
            No se pudieron cargar tus reservas.{' '}
            <button type="button" onClick={reload} className="underline">
              Reintentar
            </button>
          </p>
        ) : groups === null ? (
          <p role="status" className="text-gray-500">
            Cargando tus reservas...
          </p>
        ) : reservations?.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-300 bg-white p-10 text-center">
            <p className="text-lg font-semibold text-gray-800">Aún no tienes reservas</p>
            <Link to="/rooms" className="mt-4 inline-block rounded-md bg-orange-600 px-4 py-2 text-white hover:bg-orange-700">
              Explorar habitaciones
            </Link>
          </div>
        ) : (
          <>
            <div role="tablist" aria-label="Mis reservas" className="mb-6 flex gap-2 border-b border-gray-200">
              {BOOKING_TABS.map((tab, index) => {
                const selected = tab.id === activeTab;
                return (
                  <button
                    key={tab.id}
                    ref={(element) => {
                      tabRefs.current[tab.id] = element;
                    }}
                    type="button"
                    role="tab"
                    id={`tab-${tab.id}`}
                    aria-selected={selected}
                    aria-controls={`panel-${tab.id}`}
                    tabIndex={selected ? 0 : -1}
                    onClick={() => setActiveTab(tab.id)}
                    onKeyDown={(event) => handleTabKeyDown(event, index)}
                    className={`-mb-px border-b-2 px-4 py-2 font-medium focus:outline-none focus-visible:ring focus-visible:ring-orange-300 ${
                      selected ? 'border-orange-600 text-orange-700' : 'border-transparent text-gray-600 hover:text-gray-900'
                    }`}
                  >
                    {tab.label} ({groups[tab.id].length})
                  </button>
                );
              })}
            </div>

            <section role="tabpanel" id={`panel-${activeTab}`} aria-labelledby={`tab-${activeTab}`}>
              {visible.length === 0 ? (
                <p className="text-gray-600">{EMPTY_TAB_MESSAGES[activeTab]}</p>
              ) : (
                <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
                  {visible.map((reservation) => (
                    <ReservationCard key={reservation.id} reservation={reservation} onCancel={setToCancel} />
                  ))}
                </div>
              )}
            </section>
          </>
        )}

        {toCancel && (
          <CancelReservationModal
            reservation={toCancel}
            onClose={() => setToCancel(null)}
            onCancelled={handleCancelled}
            onStale={() => void reload()}
          />
        )}
      </main>
    </div>
  );
}
