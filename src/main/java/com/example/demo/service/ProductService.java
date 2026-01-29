package com.example.demo.service;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.ProductResponseDto;
import java.util.List;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto request);
    ProductResponseDto getProductById(String id);
    List<ProductResponseDto> getAllProducts();
    List<ProductResponseDto> searchProducts(String keyword);

    //List<ProductResponseDto> getAllProducts();
}