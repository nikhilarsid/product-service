package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query; // ✅ IMPORT ADDED
import org.springframework.stereotype.Repository;

import java.util.List; // ✅ IMPORT ADDED
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    Optional<Product> findByProductId(Integer productId);

    // Find parent product by name
    Optional<Product> findByNormalizedNameAndBrandIgnoreCase(String normalizedName, String brand);

    // ✅ NEW: Find by category (Case insensitive search inside the categories array)
    @Query("{'categories': {$regex: ?0, $options: 'i'}}")
    Page<Product> findByCategory(String category, Pageable pageable);
}