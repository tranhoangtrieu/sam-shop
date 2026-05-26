import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Order } from '../../../core/models/order.model';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { OrderService } from '../order.service';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, LoadingComponent],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss'
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  private route = inject(ActivatedRoute);

  orders: Order[] = [];
  loading = true;
  placedId: string | null = null;

  ngOnInit(): void {
    this.placedId = this.route.snapshot.queryParamMap.get('placed');
    this.orderService.myOrders().subscribe({
      next: res => {
        this.orders = res.data || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }
}
