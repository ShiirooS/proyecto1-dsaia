import api from './axiosConfig'; // Usa la instancia configurada

export interface RoomDTO {
  id: number;
  roomNumber: string;
  type: string;
  pricePerNight: number;
  availability: boolean;
  description: string;
  capacity: number;
  image: string;
  amenities: string[];
  hotelId: number;  // Opcional
}

interface CreateRoomResponse {
  success: boolean;
  room: RoomDTO;
}

/** Fecha calendario `YYYY-MM-DD`, sin zona horaria. */
export type IsoDate = string;

export interface RoomAvailability {
  roomId: number;
  from: IsoDate;
  to: IsoDate;
  today: IsoDate;
  checkInTime: string;
  maxNights: number;
  occupiedDates: IsoDate[];
}

/** Un 204 sin cuerpo llega como `''`, por eso se normaliza a `[]`. */
export async function fetchRooms(opts?: { activeOnly?: boolean }): Promise<RoomDTO[]> {
  const response = await api.get('/api/room/get-all', opts?.activeOnly ? { params: { activeOnly: true } } : undefined);
  return Array.isArray(response.data) ? (response.data as RoomDTO[]) : [];
}

export async function getAvailability(id: number | string, from: IsoDate, to: IsoDate): Promise<RoomAvailability> {
  const response = await api.get(`/api/room/get-availability/${id}`, { params: { from, to } });
  return response.data as RoomAvailability;
}

export async function createRoom(data: Partial<RoomDTO>): Promise<CreateRoomResponse> {
  const response = await api.post('/api/room/registration-room', data);
  return response.data as CreateRoomResponse;
}

export async function updateRoom(id: number, data: Partial<RoomDTO>): Promise<CreateRoomResponse> {
  const response = await api.put(`/api/room/update-room/${id}`, data);
  return response.data as CreateRoomResponse;
}

export async function deleteRoom(id: number): Promise<void> {
  await api.delete(`/api/room/delete-room/${id}`);
}

export async function getRoomById(id: number): Promise<RoomDTO> {
  const response = await api.get(`/api/room/get-room/${id}`);
  return response.data as RoomDTO;
}