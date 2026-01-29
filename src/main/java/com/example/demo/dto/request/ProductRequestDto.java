package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ProductRequestDto {

    @NotBlank(message = "Custom Product ID is required")
    private String productId;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotEmpty(message = "At least one image URL is required")
    private List<String> imageUrls;

    private Map<String, String> attributes;
}