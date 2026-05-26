package com.example.product_service.util;

import com.example.product_service.exception.AppException;
import com.example.product_service.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MultipartJsonParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public <T> T parseAndValidate(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Product data is required");
        }
        try {
            T target = objectMapper.readValue(json, type);
            Set<ConstraintViolation<T>> violations = validator.validate(target);
            if (!violations.isEmpty()) {
                String message = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new AppException(ErrorCode.BAD_REQUEST, message);
            }
            return target;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Invalid product data JSON");
        }
    }
}
