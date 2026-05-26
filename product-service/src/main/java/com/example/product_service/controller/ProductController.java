package com.example.product_service.controller;

import com.example.product_service.common.ApiResponse;
import com.example.product_service.exception.AppException;
import com.example.product_service.exception.ErrorCode;
import com.example.product_service.dto.request.ProductCreateRequest;
import com.example.product_service.dto.request.ProductUpdateRequest;
import com.example.product_service.dto.request.StockUpdateRequest;
import com.example.product_service.dto.response.ProductResponse;
import com.example.product_service.service.ProductService;
import com.example.product_service.util.MultipartJsonParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final MultipartJsonParser multipartJsonParser;

    @GetMapping
    public ApiResponse<List<ProductResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(productService.findAll(search, categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.findById(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> createWithImage(MultipartHttpServletRequest multipartRequest) {
        ProductCreateRequest request = multipartJsonParser.parseAndValidate(
                readDataJson(multipartRequest), ProductCreateRequest.class);
        return ApiResponse.ok(productService.create(request, multipartRequest.getFile("file")));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> updateWithImage(
            @PathVariable Long id,
            MultipartHttpServletRequest multipartRequest) {
        ProductUpdateRequest request = multipartJsonParser.parseAndValidate(
                readDataJson(multipartRequest), ProductUpdateRequest.class);
        return ApiResponse.ok(productService.update(id, request, multipartRequest.getFile("file")));
    }

    private String readDataJson(MultipartHttpServletRequest request) {
        String param = request.getParameter("data");
        if (param != null && !param.isBlank()) {
            return param;
        }
        MultipartFile dataFile = request.getFile("data");
        if (dataFile != null && !dataFile.isEmpty()) {
            try {
                return new String(dataFile.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Invalid product data");
            }
        }
        throw new AppException(ErrorCode.BAD_REQUEST, "Product data is required");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok("Deleted", null);
    }

    @PutMapping("/{id}/stock")
    public ApiResponse<ProductResponse> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request) {
        return ApiResponse.ok(productService.adjustStock(id, request.getDelta()));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(productService.uploadImage(id, file));
    }

    @DeleteMapping("/{id}/image")
    public ApiResponse<ProductResponse> deleteImage(@PathVariable Long id) {
        return ApiResponse.ok(productService.deleteImage(id));
    }
}
