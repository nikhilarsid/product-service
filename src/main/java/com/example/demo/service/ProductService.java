package com.example.demo.service;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.ProductDetailDto;
import com.example.demo.dto.response.ProductDisplayDto;
import java.util.List;

public interface ProductService {
    ProductDisplayDto addProduct(ProductRequestDto request);
    List<ProductDisplayDto> getAllProducts();

    // Get only the products listed by the logged-in merchant
    List<ProductDisplayDto> getMerchantProducts();

    ProductDetailDto getProductDetails(Integer productId, String variantId);

    // For Merchant Dashboard (Uses Token)
    void updateInventory(Integer productId, String variantId, Double newPrice, Integer newStock);

    // For Merchant Dashboard (Uses Token)
    void removeInventory(Integer productId, String variantId);

    // ✅ MERGED: Search and Suggest methods
    List<ProductDisplayDto> searchProducts(String query);
    List<ProductDisplayDto> suggestProducts(String query);

    // ✅ MERGED: For Order Service (Explicit Merchant ID)
    void reduceStock(Integer productId, String variantId, String merchantId, Integer quantity);
}