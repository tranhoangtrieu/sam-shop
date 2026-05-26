export interface Product {
  id: number;
  name: string;
  price: number;
  description?: string;
  image?: string;
  quantity: number;
  status: string;
  categoryId?: number;
  categoryName?: string;
  createdAt?: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
}

export interface ProductRequest {
  name: string;
  price: number;
  description?: string;
  image?: string;
  quantity: number;
  status?: string;
  categoryId?: number;
}
