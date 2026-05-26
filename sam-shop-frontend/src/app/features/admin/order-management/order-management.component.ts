import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { Order, OrderStatus } from '../../../core/models/order.model';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { OrderService } from '../../orders/order.service';
import { PaymentService } from '../../payment/payment.service';

@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, LoadingComponent],
  templateUrl: './order-management.component.html',
  styleUrl: './order-management.component.scss'
})
export class OrderManagementComponent implements OnInit {
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);

  orders: Order[] = [];
  loading = true;
  message = '';

  statuses: OrderStatus[] = ['SHIPPING', 'COMPLETED', 'CANCELLED'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.orderService.getAll().subscribe({
      next: res => {
        this.orders = res.data || [];
        this.loading = false;
      }
    });
  }

  confirm(id: number): void {
    this.orderService.confirm(id).subscribe({
      next: res => {
        this.message = res.success ? 'Đã xác nhận đơn' : res.message;
        this.load();
      },
      error: err => { this.message = err.error?.message || 'Lỗi'; }
    });
  }

  updateStatus(id: number, status: OrderStatus): void {
    if (!status) return;
    this.orderService.updateStatus(id, status).subscribe({
      next: () => this.load(),
      error: err => { this.message = err.error?.message || 'Lỗi'; }
    });
  }

  confirmCodPayment(orderId: number): void {
    this.paymentService.getByOrder(orderId).subscribe({
      next: res => {
        if (!res.success || !res.data) {
          this.message = res.message || 'Không tìm thấy thanh toán';
          return;
        }
        this.paymentService.confirmCod(res.data.id).subscribe({
          next: codRes => {
            this.message = codRes.success ? 'Đã xác nhận thu tiền COD' : codRes.message;
            this.load();
          },
          error: err => { this.message = err.error?.message || 'Xác nhận COD thất bại'; }
        });
      },
      error: err => { this.message = err.error?.message || 'Không tải thanh toán'; }
    });
  }
}
