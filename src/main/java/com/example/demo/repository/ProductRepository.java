package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    // Custom finders for Search Service integration later
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCase(String name);
}