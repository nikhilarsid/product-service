package com.example.demo.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ProductResponseDto {
    private String id;
    private String productId;
    private String name;
    private String description;
    private String category;
    private String brand;
    private List<String> imageUrls;
    private Map<String, String> attributes;
    private Boolean isActive;
}