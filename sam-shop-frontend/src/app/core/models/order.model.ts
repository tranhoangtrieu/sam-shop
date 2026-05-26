export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPING' | 'COMPLETED' | 'CANCELLED';
export type PaymentStatus = 'UNPAID' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
}

export interface Order {
  id: number;
  userId: number;
  totalPrice: number;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  shippingAddress: string;
  createdAt: string;
  items: OrderItem[];
}

export interface CreateOrderRequest {
  shippingAddress: string;
  paymentMethod?: string;
}

export interface Revenue {
  totalRevenue: number;
  paidOrderCount: number;
  pendingOrderCount: number;
}
