package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariant {
    private String variantId;
    private Map<String, String> attributes;

    // ✅ NEW: Images specific to this variant (e.g. Black vs Red)
    private List<String> imageUrls;

    private List<MerchantOffer> offers = new ArrayList<>();
}