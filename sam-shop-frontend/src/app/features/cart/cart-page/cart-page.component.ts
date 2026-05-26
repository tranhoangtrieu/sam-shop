import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Cart } from '../../../core/models/cart.model';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { CartService } from '../cart.service';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, LoadingComponent],
  templateUrl: './cart-page.component.html',
  styleUrl: './cart-page.component.scss'
})
export class CartPageComponent implements OnInit {
  private cartService = inject(CartService);

  cart: Cart | null = null;
  loading = true;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cartService.getCart().subscribe({
      next: res => {
        this.cart = res.data;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  updateQty(itemId: number, quantity: number): void {
    if (quantity < 1) return;
    this.cartService.update(itemId, quantity).subscribe({
      next: res => { this.cart = res.data; }
    });
  }

  remove(itemId: number): void {
    this.cartService.remove(itemId).subscribe({
      next: res => { this.cart = res.data; }
    });
  }
}
