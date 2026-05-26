export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
  subtotal: number;
}

export interface Cart {
  cartId: number;
  userId: number;
  items: CartItem[];
  totalPrice: number;
}
