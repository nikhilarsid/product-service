package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductDetailDto {
    private Integer productId;
    private String name;
    private String brand;
    private String description;

    // Variant specific images
    private List<String> imageUrls;

    // ✅ CHANGED: String -> List<String>
    private List<String> categories;

    // ✅ NEW: Global Specs
    private Map<String, String> specs;
    private List<String> usp;
    private String variantId;
    private Map<String, String> attributes;
    private List<MerchantOfferDto> sellers;
}