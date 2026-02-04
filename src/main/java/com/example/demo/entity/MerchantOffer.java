package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantOffer {
    private String merchantId; // Extracted from JWT
    private String merchantName; // Optional, for display
    private Double price;
    private Integer stock;
}