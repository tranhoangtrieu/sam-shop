import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Category, Product, ProductRequest } from '../../core/models/product.model';
import { ApiService } from '../../core/services/api.service';
import { ApiResponse } from '../../core/models/user.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly baseUrl = environment.apiUrl;

  constructor(
    private api: ApiService,
    private http: HttpClient
  ) {}

  getAll(search?: string, categoryId?: number) {
    const params: Record<string, string | number> = {};
    if (search) params['search'] = search;
    if (categoryId) params['categoryId'] = categoryId;
    return this.api.get<Product[]>('/api/products', params);
  }

  getById(id: number) {
    return this.api.get<Product>(`/api/products/${id}`);
  }

  create(body: ProductRequest) {
    return this.api.post<Product>('/api/products', body);
  }

  createWithImage(body: ProductRequest, file?: File | null) {
    return this.postMultipart<Product>('/api/products', body, file);
  }

  update(id: number, body: ProductRequest) {
    return this.api.put<Product>(`/api/products/${id}`, body);
  }

  updateWithImage(id: number, body: ProductRequest, file?: File | null) {
    return this.postMultipart<Product>(`/api/products/${id}`, body, file, 'PUT');
  }

  delete(id: number) {
    return this.api.delete<void>(`/api/products/${id}`);
  }

  getCategories() {
    return this.api.get<Category[]>('/api/products/categories');
  }

  uploadImage(id: number, file: File): Observable<ApiResponse<Product>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<Product>>(
      `${this.baseUrl}/api/products/${id}/image`,
      formData
    );
  }

  deleteImage(id: number): Observable<ApiResponse<Product>> {
    return this.http.delete<ApiResponse<Product>>(
      `${this.baseUrl}/api/products/${id}/image`
    );
  }

  private postMultipart<T>(
    path: string,
    body: ProductRequest,
    file?: File | null,
    method: 'POST' | 'PUT' = 'POST'
  ): Observable<ApiResponse<T>> {
    const formData = new FormData();
    formData.append('data', JSON.stringify(body));
    if (file) {
      formData.append('file', file, file.name);
    }
    const url = `${this.baseUrl}${path}`;
    return method === 'POST'
      ? this.http.post<ApiResponse<T>>(url, formData)
      : this.http.put<ApiResponse<T>>(url, formData);
  }
}
