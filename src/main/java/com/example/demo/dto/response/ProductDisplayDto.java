package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductDisplayDto {
    private Integer productId;
    private String name;
    private String brand;
    private String description;
    private String imageUrl; // Thumbnail

    // ✅ CHANGED: String -> List<String>
    private List<String> categories;

    private Map<String, String> attributes;
    private Double lowestPrice;
    private Integer totalMerchants;
    private boolean inStock;
}