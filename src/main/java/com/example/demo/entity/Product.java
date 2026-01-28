package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id; // MongoDB IDs are Strings (e.g., "650c1f...")

    private String name;
    private String description;
    private String category;
    private String brand;

    private List<String> imageUrls;

    // Flexible attributes (e.g., {"Color": "Red", "RAM": "8GB"})
    private Map<String, String> attributes;

    private Boolean isActive;
}