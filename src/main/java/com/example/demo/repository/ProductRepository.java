package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByProductId(Integer productId);

    // Find parent product by name (case insensitive logic handled in service or regex)
    Optional<Product> findByNormalizedNameAndBrandIgnoreCase(String normalizedName, String brand);
}