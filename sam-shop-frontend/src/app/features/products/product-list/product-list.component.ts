import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product } from '../../../core/models/product.model';
import { productImageSrc } from '../../../core/utils/product-image.util';
import { TokenService } from '../../../core/services/token.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { CartService } from '../../cart/cart.service';
import { ProductService } from '../product.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [RouterLink, FormsModule, CurrencyPipe, LoadingComponent],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss'
})
export class ProductListComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private token = inject(TokenService);

  products: Product[] = [];
  search = '';
  loading = true;
  message = '';
  productImageSrc = productImageSrc;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.productService.getAll(this.search || undefined).subscribe({
      next: res => {
        this.products = res.data || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  addToCart(product: Product): void {
    if (!this.token.hasRole('USER')) {
      this.message = 'Chỉ tài khoản khách hàng (USER) mới thêm giỏ hàng';
      return;
    }
    this.cartService.add(product.id, 1).subscribe({
      next: res => {
        this.message = res.success ? 'Đã thêm vào giỏ hàng!' : res.message;
      },
      error: err => { this.message = err.error?.message || 'Lỗi'; }
    });
  }
}
