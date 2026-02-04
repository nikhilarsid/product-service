package com.example.demo.controller;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ProductDetailDto;
import com.example.demo.dto.response.ProductDisplayDto;
import com.example.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 1. Create/Add Offer (Merchant Only)
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDisplayDto>> addProduct(@Valid @RequestBody ProductRequestDto request) {
        ProductDisplayDto response = productService.addProduct(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Product listing updated successfully"), HttpStatus.CREATED);
    }

    // 2. Get All Products (Flattened List for Search/Home)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDisplayDto>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts(), "Fetched all variants"));
    }

    // 3. ✅ NEW: Get Product Detail Page (Public)
    // Usage: GET /api/v1/products/101?variantId=...
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductDetails(
            @PathVariable Integer id,
            @RequestParam String variantId) {

        ProductDetailDto details = productService.getProductDetails(id, variantId);
        return ResponseEntity.ok(ApiResponse.success(details, "Product details fetched"));
    }

    // 4. ✅ NEW: Update Inventory (Merchant Only)
    // Usage: PUT /api/v1/products/inventory/101?variantId=...&price=999&stock=50
    @PutMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<Void>> updateInventory(
            @PathVariable Integer id,
            @RequestParam String variantId,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) Integer stock) {

        productService.updateInventory(id, variantId, price, stock);
        return ResponseEntity.ok(ApiResponse.success(null, "Inventory updated successfully"));
    }

    // 5. ✅ NEW: Remove Product/Offer (Merchant Only)
    // Usage: DELETE /api/v1/products/inventory/101?variantId=...
    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<Void>> removeInventory(
            @PathVariable Integer id,
            @RequestParam String variantId) {

        productService.removeInventory(id, variantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Product offer removed successfully"));
    }
}