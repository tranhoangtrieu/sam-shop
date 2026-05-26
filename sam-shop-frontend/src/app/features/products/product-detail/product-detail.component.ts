import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Product } from '../../../core/models/product.model';
import { productImageSrc } from '../../../core/utils/product-image.util';
import { TokenService } from '../../../core/services/token.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { CartService } from '../../cart/cart.service';
import { ProductService } from '../product.service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [RouterLink, FormsModule, CurrencyPipe, LoadingComponent],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss'
})
export class ProductDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private token = inject(TokenService);

  product: Product | null = null;
  productImageSrc = productImageSrc;
  loading = true;
  qty = 1;
  message = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.productService.getById(id).subscribe({
      next: res => {
        this.product = res.data;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  addToCart(): void {
    if (!this.product || !this.token.hasRole('USER')) {
      this.message = 'Vui lòng đăng nhập tài khoản USER';
      return;
    }
    this.cartService.add(this.product.id, this.qty).subscribe({
      next: res => { this.message = res.success ? 'Đã thêm vào giỏ!' : res.message; },
      error: err => { this.message = err.error?.message || 'Lỗi'; }
    });
  }
}
