import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Header from '../components/Layouts/Header'
import HeaderBanner from '../components/Layouts/HeaderBanner'
import { getRoomById, RoomDTO } from '../services/roomService';
import { fetchHotelById, HotelDTO } from '../services/hotelService';
import { FaBath, FaMapMarkerAlt, FaSnowflake, FaStar, FaTv, FaWifi } from 'react-icons/fa';
import ConfirmBookingModal from '../components/Modals/ConfirmBookingModal';
import AvailabilityCalendar, { type DateRange } from '../components/calendar/AvailabilityCalendar';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/** The selection travels in the URL (`?checkIn&checkOut&guests`) so it survives the login round-trip. */
function selectionFromUrl(params: URLSearchParams): { range: DateRange; guests: number } {
  const checkIn = params.get('checkIn');
  const checkOut = params.get('checkOut');
  const guests = Number(params.get('guests'));
  return {
    range: checkIn && checkOut && ISO_DATE.test(checkIn) && ISO_DATE.test(checkOut) && checkIn < checkOut ? { checkIn, checkOut } : {},
    guests: Number.isInteger(guests) && guests >= 1 ? guests : 1,
  };
}

export default function RoomDetails() {
  const { id } = useParams<{ id: string }>();
  return <RoomDetailsContent key={id} id={id} />;
}

function RoomDetailsContent({ id }: { id: string | undefined }) {
  const { isAuthenticated } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [initial] = useState(() => selectionFromUrl(searchParams));
  const [room, setRoom] = useState<RoomDTO | null>(null);
  const [hotel, setHotel] = useState<HotelDTO | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [range, setRange] = useState<DateRange>(initial.range);
  const [guests, setGuests] = useState(initial.guests);
  const [calendarVersion, setCalendarVersion] = useState(0);
  const [notice, setNotice] = useState<string | null>(null);
  const [resumeBookingAfterLogin, setResumeBookingAfterLogin] = useState(false);

  const closeModal = () => setIsModalOpen(false);

  useEffect(() => {
    if (resumeBookingAfterLogin && isAuthenticated) {
      setResumeBookingAfterLogin(false);
      setIsModalOpen(true);
    }
  }, [resumeBookingAfterLogin, isAuthenticated]);

  const handleBookNow = () => {
    if (isAuthenticated) {
      setNotice(null);
      setIsModalOpen(true);
      return;
    }
    if (!range.checkIn || !range.checkOut) return;
    const selection = new URLSearchParams({ checkIn: range.checkIn, checkOut: range.checkOut, guests: String(guests) });
    const redirect = `/roomDetail/${id}?${selection}`;
    setResumeBookingAfterLogin(true);
    setSearchParams(new URLSearchParams({ ...Object.fromEntries(selection), login: '1', redirect }));
  };

  const handleRoomUnavailable = () => {
    setIsModalOpen(false);
    setRange({});
    setCalendarVersion((version) => version + 1);
    setNotice('Esas fechas acaban de reservarse');
  };
  
  useEffect(() => {
    if (!id) return;

    const fetchDetails = async () => {
      try {
        const roomId = parseInt(id, 10);
        if (isNaN(roomId)) {
          setError('Invalid room ID.');
          return;
        }
        
        // Obtener detalles de la habitación
        const roomData = await getRoomById(roomId);
        setRoom(roomData);

        // Obtener detalles del hotel
        const hotelData = await fetchHotelById(roomData.hotelId);
        setHotel(hotelData);
      } catch (error) {
        console.error('Error fetching details:', error);
        setError('Failed to fetch room details. Please try again later.');
      }
    };

    fetchDetails();

    // Limpieza del efecto
    return () => {
      setRoom(null);
      setHotel(null);
      setError(null);
    };
  }, [id]);

  const booking =
    room && range.checkIn && range.checkOut
      ? {
          room,
          checkIn: range.checkIn,
          checkOut: range.checkOut,
          guests,
        }
      : null;

  if (error) {
    return <div>{error}</div>;
  }

  if (!room || !hotel) {
    return <div>Loading...</div>;
  }
  
  return (
    <div>
    <Header />
        <HeaderBanner/>
        <main className="py-12 px-4 sm:px-6 lg:px-8"> 
            <div className="max-w-6xl mx-auto">
              <div className="bg-white shadow-xl rounded-lg overflow-hidden">
                <div className="md:flex">
                  {/* Columna de la imagen */}
                  <div className="md:flex-shrink-0"> 
                    <img
                      src={room.image}
                      alt={room.type}
                      className="h-full w-full object-cover"
                    />
                  </div>
                  {/* Columna de los datos */}
                  <div className="p-8 flex-1 flex flex-col justify-between"> 
                    <div>
                      <div className="uppercase tracking-wide text-2xl sm:text-3xl text-gray-900 font-bold">
                        {room.type} - Room {room.roomNumber}
                      </div>
                      <p className="mt-2 text-4xl leading-8 font-extrabold tracking-tight text-gray-900 sm:text-5xl">
                        ${room.pricePerNight} <span className="text-base font-normal text-gray-500">/ night</span>
                      </p>
                      <div className="mt-2 flex items-center">
                        <FaStar className="text-yellow-400" />
                        <span className="ml-1 text-gray-500">{hotel.rating} / 5</span>
                      </div>

                      <div className="mt-4">
                        <h3 className="text-lg leading-6 font-medium text-gray-900">Amenities</h3>
                        <ul className="mt-2 flex flex-wrap gap-4">
                          {room.amenities.map((amenity, index) => (
                            <li key={index} className="flex items-center text-gray-500">
                              {amenity === 'Free WiFi' && <FaWifi className="mr-2" />}
                              {amenity === 'Air Conditioning' && <FaSnowflake className="mr-2" />}
                              {amenity === 'Jacuzzi' && <FaBath className="mr-2" />}
                              {amenity === 'Balcony' && <FaTv className="mr-2" />}
                              {amenity}
                            </li>
                          ))}
                        </ul>
                      </div>

                      {/* Disposición en 2 columnas para Location, Province, Capacity */}
                      <div className="mt-6 grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-2 flex items-center">
                        <div className="sm:col-span-1 mb-4">
                          <h3 className="text-lg leading-6 font-medium text-gray-900">
                            <FaMapMarkerAlt className="inline mr-2" />Location
                          </h3>
                          <p className="mt-1 text-gray-500">{hotel.location}</p>
                        </div>
                        <div className="sm:col-span-1 mb-4">
                          <h3 className="text-lg leading-6 font-medium text-gray-900">Province</h3>
                          <p className="mt-1 text-gray-500">{hotel.province}</p>
                        </div>
                        <div className="sm:col-span-1 mb-4">
                          <h3 className="text-lg leading-6 font-medium text-gray-900">Capacity</h3>
                          <p className="mt-1 text-gray-500">{room.capacity} Guests</p>
                        </div>
                      </div>

                      {/* Nueva sección para la descripción */}
                      <div className="mb-4">
                        <h3 className="text-lg leading-6 font-medium text-gray-900">Description</h3>
                        <p className="mt-1 text-gray-500">{room.description}</p>
                      </div>
                    </div>

                    <div className="mt-8 space-y-4">
                      {notice && (
                        <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700">{notice}</p>
                      )}
                      <AvailabilityCalendar
                        key={calendarVersion}
                        roomId={room.id}
                        pricePerNight={room.pricePerNight}
                        value={range}
                        onChange={setRange}
                      />
                      <div>
                        <label htmlFor="guests" className="block text-lg font-medium text-gray-900">Guests</label>
                        <select
                          id="guests"
                          value={guests}
                          onChange={(e) => setGuests(Number(e.target.value))}
                          className="mt-1 w-full rounded-md border border-gray-300 p-2"
                        >
                          {Array.from({ length: room.capacity }, (_, i) => i + 1).map((n) => (
                            <option key={n} value={n}>{n}</option>
                          ))}
                        </select>
                      </div>
                      <button
                        type="button"
                        disabled={!booking}
                        onClick={handleBookNow}
                        className="w-full rounded-lg bg-orange-600 px-5 py-2.5 text-center text-sm font-medium text-white transition hover:bg-orange-700 focus:outline-none focus:ring focus:ring-orange-400 disabled:cursor-not-allowed disabled:bg-gray-300"
                      >
                        Book now
                      </button>
                      {isModalOpen && booking && (
                        <ConfirmBookingModal isOpen={isModalOpen} closeModal={closeModal} booking={booking} onRoomUnavailable={handleRoomUnavailable} />
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </main>



    </div>
  )
}
