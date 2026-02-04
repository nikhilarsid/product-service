package com.example.demo.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ProductRequestDto {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Brand is required")
    private String brand;

    private String description;

    // ✅ CHANGED: String -> List<String>
    @NotNull(message = "Categories are required")
    private List<String> categories;

    // This will now be mapped to the Variant's imageUrls
    private List<String> imageUrls;

    @NotNull(message = "Attributes are required")
    private Map<String, String> attributes;

    // ✅ NEW: Global technical specs
    private Map<String, String> specs;

    // Inventory Details
    @NotNull
    @Min(0)
    private Double price;

    @NotNull
    @Min(0)
    private Integer quantity;
}