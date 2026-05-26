package com.example.product_service.repository;

import com.example.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category")
    List<Product> findAllWithCategory();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId")
    List<Product> findAllWithCategoryByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            SELECT p FROM Product p LEFT JOIN FETCH p.category
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            """)
    List<Product> searchByName(
            @Param("search") String search,
            @Param("categoryId") Long categoryId);
}
