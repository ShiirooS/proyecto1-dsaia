import Button from './Button'
import type { RoomSummary } from '../types/api'
import { formatPrice } from '../utils/format'

interface CardRoomProps {
  room: RoomSummary
}

export default function CardRoom({ room }: CardRoomProps) {
  return (
    <article className="relative overflow-hidden rounded-lg shadow-md group hover:shadow-xl hover:-translate-y-2 transition-transform duration-300 ease-in-out bg-white">
        {room.imageUrl ? (
          <img
              src={room.imageUrl}
              alt={`Habitación ${room.roomNumber} - ${room.hotel.name}`}
              width={500}
              height={400}
              loading="lazy"
              className="object-cover w-full h-64"
          />
        ) : (
          <div role="img" aria-label="Sin imagen" className="flex h-64 w-full items-center justify-center bg-gray-100 text-gray-400">Sin imagen</div>
        )}
        <div className="p-4 space-y-4">
            <div className="flex items-center justify-between gap-2">
                <h3 className="text-lg font-semibold text-black">{room.hotel.name}</h3>
                <div className="text-2xl font-bold text-black">{formatPrice(room.pricePerNight)}<span className="text-sm font-normal text-gray-500"> / noche</span></div>
            </div>
            <p className="text-gray-700">{room.type} · Habitación {room.roomNumber} · {room.capacity} huéspedes</p>
            <p className="text-gray-500">{room.hotel.location}</p>
            <div className="mt-4 md:mt-8 ">
                <Button to={`/rooms/${room.id}`} content="Ver disponibilidad" className="block w-full px-12 py-3 text-center"/>
            </div>
        </div>
    </article>
  )
}
