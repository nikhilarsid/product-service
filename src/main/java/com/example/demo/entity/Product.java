package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Transient
    public static final String SEQUENCE_NAME = "product_sequence";

    @Id
    private String id;

    @Indexed(unique = true)
    private Integer productId;

    @Indexed
    private String normalizedName;

    private String name;
    private String brand;
    private String description;

    // ✅ CHANGED: String -> List<String>
    private List<String> categories;

    private Boolean isActive;

    // ✅ NEW: Timestamps for "Newest" sort
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ✅ NEW: Global Specs (e.g. Screen Size, Processor)
    private Map<String, String> specs;

    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();
}