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

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDisplayDto>> addProduct(@Valid @RequestBody ProductRequestDto request) {
        ProductDisplayDto response = productService.addProduct(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Product listing updated successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDisplayDto>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts(), "Fetched all variants"));
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

    // ✅ MERGED: Search and Suggest (From Remote)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductDisplayDto>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(q), "Search results fetched"));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<ProductDisplayDto>>> suggest(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(productService.suggestProducts(q), "Suggestions fetched"));
    }

    // ✅ MERGED: Reduce Stock (Your Local Change)
    @PutMapping("/reduce-stock/{id}")
    public ResponseEntity<ApiResponse<Void>> reduceStock(
            @PathVariable Integer id,
            @RequestParam String variantId,
            @RequestParam String merchantId,
            @RequestParam Integer quantity) {

        productService.reduceStock(id, variantId, merchantId, quantity);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock reduced successfully"));
    }
}