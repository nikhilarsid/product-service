package com.example.demo.service.impl;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.ProductResponseDto;
import com.example.demo.entity.Product;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.bson.Document; // ✅ Added
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate; // ✅ Added
import org.springframework.data.mongodb.core.aggregation.Aggregation; // ✅ Added
import org.springframework.stereotype.Service;

import java.util.Arrays; // ✅ Added
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate; // ✅ Added for Atlas Search

    @Override
    public ProductResponseDto createProduct(ProductRequestDto request) {
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        product.setIsActive(true);

        Product savedProduct = productRepository.save(product);
        return mapToDto(savedProduct);
    }

    @Override
    public ProductResponseDto getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToDto(product);
    }

    @Override
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ✅ UPDATED: Now uses MongoDB Atlas Search (Index: "default")
    @Override
    public List<ProductResponseDto> searchProducts(String keyword) {
        Document searchStage = new Document("$search",
                new Document("index", "default")
                        .append("compound", new Document()
                                .append("should", Arrays.asList(
                                        // 1. High Priority: Autocomplete on Name (Handles Typos like "iphoen")
                                        new Document("autocomplete", new Document()
                                                .append("query", keyword)
                                                .append("path", "name")
                                                .append("fuzzy", new Document("maxEdits", 2))
                                                .append("score", new Document("boost", new Document("value", 10)))
                                        ),

                                        // 2. Medium Priority: Brand or Category
                                        new Document("text", new Document()
                                                .append("query", keyword)
                                                .append("path", Arrays.asList("brand", "category"))
                                                .append("score", new Document("boost", new Document("value", 5)))
                                        ),

                                        // 3. Low Priority: Description or Attributes (specs)
                                        new Document("text", new Document()
                                                .append("query", keyword)
                                                .append("path", Arrays.asList("description", "attributes.*")) // Wildcard for map
                                                .append("score", new Document("boost", new Document("value", 1)))
                                        )
                                ))
                                .append("minimumShouldMatch", 1)
                        )
        );

        // Run Aggregation
        Aggregation aggregation = Aggregation.newAggregation(
                context -> searchStage,
                Aggregation.limit(20) // Limit to 20 results
        );

        // Convert Results to DTOs
        return mongoTemplate.aggregate(aggregation, "products", Product.class)
                .getMappedResults().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ProductResponseDto mapToDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        BeanUtils.copyProperties(product, dto);
        return dto;
    }
}