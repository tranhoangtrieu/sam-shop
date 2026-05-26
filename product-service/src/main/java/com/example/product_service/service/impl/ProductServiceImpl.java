package com.example.product_service.service.impl;

import com.example.product_service.dto.request.ProductCreateRequest;
import com.example.product_service.dto.request.ProductUpdateRequest;
import com.example.product_service.dto.response.ProductResponse;
import com.example.product_service.entity.Category;
import com.example.product_service.entity.Product;
import com.example.product_service.enums.ProductStatus;
import com.example.product_service.exception.AppException;
import com.example.product_service.exception.ErrorCode;
import com.example.product_service.mapper.ProductMapper;
import com.example.product_service.repository.CategoryRepository;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.service.ProductImageStorageService;
import com.example.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductImageStorageService imageStorage;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(String search, Long categoryId) {
        String term = (search == null || search.isBlank()) ? null : search.trim();
        List<Product> products;
        if (term == null) {
            products = categoryId == null
                    ? productRepository.findAllWithCategory()
                    : productRepository.findAllWithCategoryByCategoryId(categoryId);
        } else {
            products = productRepository.searchByName(term, categoryId);
        }
        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(getProduct(id));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        return create(request, null);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request, MultipartFile imageFile) {
        Category category = resolveCategory(request.getCategoryId());
        ProductStatus status = request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE;
        if (request.getQuantity() != null && request.getQuantity() <= 0) {
            status = ProductStatus.OUT_OF_STOCK;
        }
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .status(status)
                .category(category)
                .build();
        product = productRepository.save(product);
        return productMapper.toResponse(applyImageFile(product, imageFile));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        return update(id, request, null);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request, MultipartFile imageFile) {
        Product product = getProduct(id);
        if (request.getName() != null) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getQuantity() != null) {
            product.setQuantity(request.getQuantity());
            if (request.getQuantity() <= 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            }
        }
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getCategoryId() != null) {
            product.setCategory(resolveCategory(request.getCategoryId()));
        }
        product = productRepository.save(product);
        return productMapper.toResponse(applyImageFile(product, imageFile));
    }

    @Override
    @Transactional
    public ProductResponse uploadImage(Long id, MultipartFile file) {
        Product product = getProduct(id);
        String previous = product.getImage();
        String imageUrl = imageStorage.store(id, file);
        product.setImage(imageUrl);
        Product saved = productRepository.save(product);
        imageStorage.deleteIfExists(previous);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse deleteImage(Long id) {
        Product product = getProduct(id);
        imageStorage.deleteIfExists(product.getImage());
        product.setImage(null);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Product not found"));
        imageStorage.deleteIfExists(product.getImage());
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public ProductResponse adjustStock(Long id, int delta) {
        Product product = getProduct(id);
        int newQty = product.getQuantity() + delta;
        if (newQty < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        product.setQuantity(newQty);
        if (newQty == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasStock(Long id, int quantity) {
        Product product = getProduct(id);
        return product.getQuantity() >= quantity && product.getStatus() == ProductStatus.ACTIVE;
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Product not found"));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Category not found"));
    }

    private Product applyImageFile(Product product, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return product;
        }
        String previous = product.getImage();
        String imageUrl = imageStorage.store(product.getId(), imageFile);
        product.setImage(imageUrl);
        Product saved = productRepository.save(product);
        imageStorage.deleteIfExists(previous);
        return saved;
    }
}
