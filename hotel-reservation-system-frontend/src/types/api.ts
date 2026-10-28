/** Fecha calendario sin zona horaria, formato YYYY-MM-DD. */
export type IsoDate = string

export interface HotelSummary {
  id: number
  name: string
  location: string
}

export interface HotelDetail extends HotelSummary {
  description: string | null
  rating: number | null
}

export interface RoomSummary {
  id: number
  roomNumber: string
  type: string
  pricePerNight: number
  capacity: number
  imageUrl: string | null
  hotel: HotelSummary
}

export interface RoomDetail extends Omit<RoomSummary, 'hotel'> {
  description: string | null
  hotel: HotelDetail
}

export interface RoomAvailability {
  roomId: number
  from: IsoDate
  to: IsoDate
  /** Hoy en la zona horaria del hotel. */
  today: IsoDate
  checkInTime: string
  maxNights: number
  /** Cada fecha es una NOCHE ocupada (la noche que empieza ese día). */
  occupiedDates: IsoDate[]
}
