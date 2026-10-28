import api from './axiosConfig';
import { toPathId } from './pathId';

export type CardBrand = 'VISA' | 'MASTERCARD' | 'AMEX';

export interface PaymentMethod {
  id: number;
  brand: CardBrand;
  last4: string;
  expMonth: number;
  expYear: number;
  holderName: string;
  isDefault: boolean;
  expired: boolean;
  createdAt: string;
}

export interface CardInput {
  cardNumber: string;
  expMonth: number;
  expYear: number;
  cvv: string;
  holderName: string;
}

export async function getPaymentMethods(): Promise<PaymentMethod[]> {
  const response = await api.get('/api/payment-method/get-all');
  return Array.isArray(response.data) ? (response.data as PaymentMethod[]) : [];
}

export async function createPaymentMethod(card: CardInput, makeDefault: boolean): Promise<PaymentMethod> {
  const response = await api.post('/api/payment-method/registration-payment-method', { ...card, makeDefault });
  return response.data as PaymentMethod;
}

export async function setDefaultPaymentMethod(id: number): Promise<PaymentMethod> {
  const response = await api.put(`/api/payment-method/set-default/${toPathId(id)}`);
  return response.data as PaymentMethod;
}

export async function deletePaymentMethod(id: number): Promise<void> {
  await api.delete(`/api/payment-method/delete-payment-method/${toPathId(id)}`);
}
