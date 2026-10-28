import { apiGet } from './client'
import type { IsoDate, RoomAvailability, RoomDetail, RoomSummary } from '../types/api'

export const getRooms = (hotelId?: number) =>
  apiGet<RoomSummary[]>(hotelId === undefined ? '/api/rooms' : `/api/rooms?hotelId=${hotelId}`)

export const getRoom = (id: number | string) => apiGet<RoomDetail>(`/api/rooms/${id}`)

export const getAvailability = (id: number | string, from: IsoDate, to: IsoDate) =>
  apiGet<RoomAvailability>(`/api/rooms/${id}/availability?from=${from}&to=${to}`)
