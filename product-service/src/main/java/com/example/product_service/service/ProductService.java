package com.example.product_service.service;

import com.example.product_service.dto.request.ProductCreateRequest;
import com.example.product_service.dto.request.ProductUpdateRequest;
import com.example.product_service.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    List<ProductResponse> findAll(String search, Long categoryId);
    ProductResponse findById(Long id);
    ProductResponse create(ProductCreateRequest request);

    ProductResponse create(ProductCreateRequest request, MultipartFile imageFile);

    ProductResponse update(Long id, ProductUpdateRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request, MultipartFile imageFile);
    ProductResponse uploadImage(Long id, MultipartFile file);

    ProductResponse deleteImage(Long id);

    void delete(Long id);
    ProductResponse adjustStock(Long id, int delta);
    boolean hasStock(Long id, int quantity);
}
