import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { Revenue } from '../../../core/models/order.model';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { OrderService } from '../../orders/order.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CurrencyPipe, LoadingComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private orderService = inject(OrderService);

  revenue: Revenue | null = null;
  loading = true;

  ngOnInit(): void {
    this.orderService.getRevenue().subscribe({
      next: res => {
        this.revenue = res.data;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }
}
