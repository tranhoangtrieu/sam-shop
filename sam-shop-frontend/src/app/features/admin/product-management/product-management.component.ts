import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Product } from '../../../core/models/product.model';
import { TokenService } from '../../../core/services/token.service';
import { productImageSrc } from '../../../core/utils/product-image.util';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { ProductService } from '../../products/product.service';

const MAX_IMAGE_BYTES = 2 * 1024 * 1024;
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, LoadingComponent],
  templateUrl: './product-management.component.html',
  styleUrl: './product-management.component.scss'
})
export class ProductManagementComponent implements OnInit, OnDestroy {
  private productService = inject(ProductService);
  private token = inject(TokenService);
  private fb = inject(FormBuilder);

  products: Product[] = [];
  loading = true;
  message = '';
  imageError = '';
  showForm = false;
  editId: number | null = null;
  imagePreview: string | null = null;
  pendingImageFile: File | null = null;
  removeImage = false;
  private objectPreviewUrl: string | null = null;

  isAdmin = this.token.hasRole('ADMIN');

  form = this.fb.group({
    name: ['', Validators.required],
    price: [25000, [Validators.required, Validators.min(1)]],
    description: [''],
    quantity: [100, Validators.required],
    categoryId: [1]
  });

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.revokeObjectPreview();
  }

  load(): void {
    this.loading = true;
    this.productService.getAll().subscribe({
      next: res => {
        this.products = res.data || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getImage(product: Product): string | null {
    return productImageSrc(product.image);
  }

  openCreate(): void {
    this.editId = null;
    this.resetImageState();
    this.form.reset({ name: '', price: 25000, description: '', quantity: 100, categoryId: 1 });
    this.showForm = true;
  }

  openEdit(p: Product): void {
    this.editId = p.id;
    this.resetImageState();
    this.form.patchValue({
      name: p.name,
      price: p.price,
      description: p.description,
      quantity: p.quantity,
      categoryId: p.categoryId
    });
    this.imagePreview = productImageSrc(p.image);
    this.showForm = true;
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.imageError = '';
    if (!file) {
      return;
    }
    if (!ALLOWED_TYPES.includes(file.type)) {
      this.imageError = 'Chỉ chấp nhận JPEG, PNG, WebP hoặc GIF';
      input.value = '';
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      this.imageError = 'Ảnh tối đa 2MB';
      input.value = '';
      return;
    }
    this.removeImage = false;
    this.pendingImageFile = file;
    this.revokeObjectPreview();
    this.objectPreviewUrl = URL.createObjectURL(file);
    this.imagePreview = this.objectPreviewUrl;
  }

  clearImage(): void {
    this.removeImage = true;
    this.pendingImageFile = null;
    this.revokeObjectPreview();
    this.imagePreview = null;
  }

  save(): void {
    if (this.form.invalid) return;
    const body = { ...this.form.getRawValue(), status: 'ACTIVE' };

    const onSuccess = (res: { success?: boolean; message?: string }) => {
      this.message = res.success !== false ? 'Lưu thành công' : (res.message ?? 'Lỗi');
      this.showForm = false;
      this.load();
    };

    const onError = (err: { error?: { message?: string } }) => {
      this.message = err.error?.message || 'Lỗi lưu sản phẩm';
    };

    const file = this.removeImage ? null : this.pendingImageFile;

    if (this.editId) {
      this.productService.updateWithImage(this.editId, body as never, file).subscribe({
        next: res => {
          if (this.removeImage) {
            this.productService.deleteImage(this.editId!).subscribe({ next: onSuccess, error: onError });
          } else {
            onSuccess(res);
          }
        },
        error: onError
      });
      return;
    }

    this.productService.createWithImage(body as never, file).subscribe({
      next: onSuccess,
      error: onError
    });
  }

  delete(id: number): void {
    if (!confirm('Xóa sản phẩm này?')) return;
    this.productService.delete(id).subscribe({
      next: () => this.load(),
      error: err => { this.message = err.error?.message || 'Chỉ ADMIN mới xóa được'; }
    });
  }

  private resetImageState(): void {
    this.revokeObjectPreview();
    this.imagePreview = null;
    this.pendingImageFile = null;
    this.removeImage = false;
    this.imageError = '';
  }

  private revokeObjectPreview(): void {
    if (this.objectPreviewUrl) {
      URL.revokeObjectURL(this.objectPreviewUrl);
      this.objectPreviewUrl = null;
    }
  }
}
