package com.example.demo.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ProductResponseDto {
    private String id;
    private Integer productId;
    private String name;
    private String description;

    // ❌ REMOVE: private String category;
    // ✅ ADD: Must match Entity field name exactly for BeanUtils
    private List<String> categories;

    private String brand;
    private List<String> imageUrls;
    private Map<String, String> attributes;
    private List<String> usp;
    private Boolean isActive;
    private boolean inStock;
}