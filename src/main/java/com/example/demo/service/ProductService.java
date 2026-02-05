package com.example.demo.service;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.ProductDetailDto;
import com.example.demo.dto.response.ProductDisplayDto;
import java.util.List;
// Add these imports at the top
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;

// Inside ProductServiceImpl class:
public interface ProductService {
    ProductDisplayDto addProduct(ProductRequestDto request);
    List<ProductDisplayDto> getAllProducts();
    List<ProductDisplayDto> getMerchantProducts();
    ProductDetailDto getProductDetails(Integer productId, String variantId);
    void updateInventory(Integer productId, String variantId, Double newPrice, Integer newStock);
    void removeInventory(Integer productId, String variantId);

    // ✅ NEW: Search and Suggest methods
    List<ProductDisplayDto> searchProducts(String query);
    List<ProductDisplayDto> suggestProducts(String query);
}