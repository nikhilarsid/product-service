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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDisplayDto>> addProduct(@Valid @RequestBody ProductRequestDto request) {
        ProductDisplayDto response = productService.addProduct(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Product listing updated successfully"), HttpStatus.CREATED);
    }


    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDisplayDto>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ProductDisplayDto> products;

        // 1. If category is present, use the category + pagination logic
        if (category != null && !category.trim().isEmpty()) {
            products = productService.getProductsByCategory(category, pageable);
        } else {
            // 2. Otherwise, use the standard pagination logic
            products = productService.getAllProducts(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(products, "Fetched products successfully"));
    }

    

    @GetMapping("/my-listings")
    public ResponseEntity<ApiResponse<List<ProductDisplayDto>>> getMyListings() {
        List<ProductDisplayDto> myListings = productService.getMerchantProducts();
        return ResponseEntity.ok(ApiResponse.success(myListings, "Merchant listings fetched"));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductDetails(
            @PathVariable Integer id,
            @RequestParam String variantId) {
        ProductDetailDto details = productService.getProductDetails(id, variantId);
        return ResponseEntity.ok(ApiResponse.success(details, "Product details fetched"));
    }

    @PutMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<Void>> updateInventory(
            @PathVariable Integer id,
            @RequestParam String variantId,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) Integer stock) {
        productService.updateInventory(id, variantId, price, stock);
        return ResponseEntity.ok(ApiResponse.success(null, "Inventory updated successfully"));
    }

    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<Void>> removeInventory(
            @PathVariable Integer id,
            @RequestParam String variantId) {
        productService.removeInventory(id, variantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Product offer removed successfully"));
    }

    // ✅ MERGED: Search and Suggest
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductDisplayDto>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(q), "Search results fetched"));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<ProductDisplayDto>>> suggest(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(productService.suggestProducts(q), "Suggestions fetched"));
    }

    // ✅ MERGED: Reduce Stock
    @PutMapping("/reduce-stock/{id}")
    public ResponseEntity<ApiResponse<Void>> reduceStock(
            @PathVariable Integer id,
            @RequestParam String variantId,
            @RequestParam String merchantId,
            @RequestParam Integer quantity) {
        productService.reduceStock(id, variantId, merchantId, quantity);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock reduced successfully"));
    } // Added missing closing brace

    @PostMapping("/migrate-usps")
    public ResponseEntity<ApiResponse<String>> migrateUsps() {
        productService.populateRandomUSPs();
        return ResponseEntity.ok(ApiResponse.success(null, "Random USPs assigned to existing products"));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyProduct(
            @RequestParam String name,
            @RequestParam String brand) {

        // Log the incoming check for debugging
//        log.info("🔍 [VERIFY] Checking existence for Name: {} and Brand: {}", name, brand);
        // Hardcoded to return true for now as requested
        boolean exists = true;
        return ResponseEntity.ok(ApiResponse.success(exists, "Product existence verified (Mocked)"));
    }
}