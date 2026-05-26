import { Injectable } from '@angular/core';
import { CreateOrderRequest, Order, OrderStatus, Revenue } from '../../core/models/order.model';
import { ApiService } from '../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class OrderService {
  constructor(private api: ApiService) {}

  create(body: CreateOrderRequest) {
    return this.api.post<Order>('/api/orders', body);
  }

  myOrders() {
    return this.api.get<Order[]>('/api/orders');
  }

  getById(id: number) {
    return this.api.get<Order>(`/api/orders/${id}`);
  }

  getAll() {
    return this.api.get<Order[]>('/api/orders/all');
  }

  confirm(id: number) {
    return this.api.put<Order>(`/api/orders/${id}/confirm`, {});
  }

  updateStatus(id: number, status: OrderStatus) {
    return this.api.put<Order>(`/api/orders/${id}/status`, { status });
  }

  getRevenue() {
    return this.api.get<Revenue>('/api/orders/revenue');
  }
}
