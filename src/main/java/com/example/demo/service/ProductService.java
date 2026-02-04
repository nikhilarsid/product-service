package com.example.demo.service;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.ProductDetailDto;
import com.example.demo.dto.response.ProductDisplayDto;
import java.util.List;

public interface ProductService {
    ProductDisplayDto addProduct(ProductRequestDto request);
    List<ProductDisplayDto> getAllProducts();

    // ✅ NEW: Get only the products listed by the logged-in merchant
    List<ProductDisplayDto> getMerchantProducts();

    ProductDetailDto getProductDetails(Integer productId, String variantId);

    void updateInventory(Integer productId, String variantId, Double newPrice, Integer newStock);
    void removeInventory(Integer productId, String variantId);
}