import { Injectable } from '@angular/core';
import { Cart } from '../../core/models/cart.model';
import { ApiService } from '../../core/services/api.service';
import { TokenService } from '../../core/services/token.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  constructor(private api: ApiService, private tokenService: TokenService) {}

  add(productId: number, quantity: number) {
    return this.api.post<Cart>('/api/cart/add', { productId, quantity });
  }

  getCart() {
    const userId = this.tokenService.getUser()?.userId;
    return this.api.get<Cart>(`/api/cart/${userId}`);
  }

  update(itemId: number, quantity: number) {
    return this.api.put<Cart>('/api/cart/update', { itemId, quantity });
  }

  remove(itemId: number) {
    return this.api.delete<Cart>(`/api/cart/remove/${itemId}`);
  }
}
