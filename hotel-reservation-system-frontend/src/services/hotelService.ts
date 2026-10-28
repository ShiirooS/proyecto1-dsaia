import { toPathId } from './pathId';

import api from './axiosConfig';

export interface HotelDTO {
    id: number;
    name: string;
    location: string;          
    description: string;       
    rating: number;            
    image: string;       
    province: string;    
    roomIds?: number[];         // Opcional, como en el DTO de Java, lista de IDs de habitaciones
    reviews?: ReviewDTO[];      // Opcional, como en el DTO de Java, lista de reseñas (asegúrate de definir ReviewDTO también)
  }
  
  export interface ReviewDTO {
    id?: number;
    content?: string;
    rating?: number;
    // Otros campos de reseña según sea necesario
  }
  
export async function fetchHotels(): Promise<HotelDTO[]> {
  const response = await api.get('/api/hotel/get-all');
  return response.data as HotelDTO[];
}

export async function createHotel(data: HotelDTO): Promise<void> {
  await api.post('/api/hotel/registration-hotel', data);
}

export async function updateHotel(id: number, data: HotelDTO): Promise<void> {
  await api.put(`/api/hotel/update-hotel/${toPathId(id)}`, data);
}

export async function deleteHotel(id: number): Promise<void> {
  await api.delete(`/api/hotel/delete-hotel/${toPathId(id)}`);
}

export async function fetchHotelById(id: number): Promise<HotelDTO> {
  const response = await api.get(`/api/hotel/get-hotel/${toPathId(id)}`);
  return response.data as HotelDTO;
}
