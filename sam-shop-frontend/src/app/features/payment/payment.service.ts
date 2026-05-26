import { Injectable } from '@angular/core';
import { ApiService } from '../../core/services/api.service';

export interface Payment {
  id: number;
  orderId: number;
  userId: string | number;
  amount: number;
  paymentMethod: string;
  paymentStatus: string;
  transactionCode?: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private api: ApiService) {}

  getByOrder(orderId: number) {
    return this.api.get<Payment>(`/api/payments/order/${orderId}`);
  }

  initiateOnline(paymentId: number) {
    return this.api.post<{ paymentUrl?: string; token?: string }>('/api/payments/online/initiate', { paymentId });
  }

  callbackOnline(paymentId: number, success: boolean) {
    return this.api.post<Payment>('/api/payments/online/callback', { paymentId, success });
  }

  confirmCod(paymentId: number) {
    return this.api.put<Payment>(`/api/payments/${paymentId}/confirm-cod`, {});
  }
}
