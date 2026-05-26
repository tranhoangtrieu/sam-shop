import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { OrderService } from '../../orders/order.service';
import { PaymentService } from '../payment.service';
import { CreateOrderRequest } from '../../../core/models/order.model';

@Component({
  selector: 'app-payment-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './payment-page.component.html',
  styleUrl: './payment-page.component.scss'
})
export class PaymentPageComponent {
  private fb = inject(FormBuilder);
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);
  private router = inject(Router);

  loading = false;
  error = '';

  form = this.fb.group({
    shippingAddress: ['', Validators.required],
    paymentMethod: ['COD', Validators.required]
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    const body = this.form.getRawValue() as CreateOrderRequest;
    this.orderService.create(body).subscribe({
      next: res => {
        if (!res.success || !res.data) {
          this.loading = false;
          this.error = res.message;
          return;
        }
        if (body.paymentMethod === 'ONLINE') {
          this.paymentService.getByOrder(res.data.id).subscribe({
            next: payRes => {
              if (!payRes.success || !payRes.data) {
                this.loading = false;
                this.error = payRes.message || 'Không tìm thấy thanh toán';
                return;
              }
              this.paymentService.initiateOnline(payRes.data.id).subscribe({
                next: initRes => {
                  if (!initRes.success) {
                    this.loading = false;
                    this.error = initRes.message;
                    return;
                  }
                  this.paymentService.callbackOnline(payRes.data.id, true).subscribe({
                    next: () => {
                      this.loading = false;
                      this.router.navigate(['/orders'], { queryParams: { placed: res.data?.id } });
                    },
                    error: () => {
                      this.loading = false;
                      this.error = 'Thanh toán online thất bại';
                    }
                  });
                },
                error: () => {
                  this.loading = false;
                  this.error = 'Không khởi tạo được thanh toán online';
                }
              });
            },
            error: () => {
              this.loading = false;
              this.error = 'Không tải thông tin thanh toán';
            }
          });
          return;
        }
        this.loading = false;
        this.router.navigate(['/orders'], { queryParams: { placed: res.data.id } });
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Đặt hàng thất bại';
      }
    });
  }
}
