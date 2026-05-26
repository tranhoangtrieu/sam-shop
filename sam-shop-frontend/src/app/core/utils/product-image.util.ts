import { environment } from '../../../environments/environment';

/**
 * Resolves product.image for &lt;img src&gt;.
 * DB stores a path such as /api/products/uploads/product-1-abc.jpg
 */
export function productImageSrc(image?: string | null): string | null {
  if (!image?.trim()) {
    return null;
  }
  const value = image.trim();
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) {
    return value;
  }
  const base = environment.apiUrl.replace(/\/$/, '');
  return value.startsWith('/') ? `${base}${value}` : `${base}/${value}`;
}
