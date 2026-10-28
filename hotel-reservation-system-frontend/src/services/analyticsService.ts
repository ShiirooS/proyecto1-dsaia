import api from './axiosConfig';

export type InventoryAction = 'INCREASE' | 'KEEP' | 'REDUCE';

/** Rates are 0..1 with 4 decimals; months are 'YYYY-MM' (API-CONTRACT §1 and §10.1). */
export interface DemandForecast {
  generatedAt: string;
  hotelId: number | null;
  horizon: number;
  activeRooms: number;
  averagePricePerNight: number;
  historyFrom: string;
  historyTo: string;
  model: {
    type: 'LINEAR_TREND_MONTHLY_SEASONALITY';
    intercept: number;
    slope: number;
    seasonalIndices: { month: number; index: number }[];
  };
  history: {
    month: string;
    occupiedRoomNights: number;
    availableRoomNights: number;
    occupancyRate: number;
    trendRate: number;
  }[];
  forecast: {
    month: string;
    forecastOccupancyRate: number;
    forecastRoomNights: number;
    availableRoomNights: number;
    bookedRoomNights: number;
    bookedOccupancyRate: number;
    priceAdjustmentPct: number;
    suggestedPricePerNight: number;
    inventoryAction: InventoryAction;
    recommendedActiveRooms: number;
  }[];
}

export interface DemandForecastQuery {
  horizon: number;
  hotelId?: number | null;
}

/** HU-03 (ADMIN): 422 INSUFFICIENT_HISTORY when there is nothing to learn from. */
export async function getDemandForecast({ horizon, hotelId }: DemandForecastQuery): Promise<DemandForecast> {
  const params: Record<string, number> = { horizon };
  if (hotelId != null) params.hotelId = hotelId;
  const response = await api.get('/api/analytics/demand-forecast', { params });
  return response.data as DemandForecast;
}
