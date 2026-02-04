package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantOfferDto {
    private String merchantId;
    private String merchantName;
    private Double price;
    private Integer stock;
}