package com.example.demo.service.impl;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.MerchantOfferDto;
import com.example.demo.dto.response.ProductDetailDto;
import com.example.demo.dto.response.ProductDisplayDto;
import com.example.demo.entity.MerchantOffer;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductVariant;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ProductService;
import com.example.demo.service.SequenceGeneratorService;
import com.example.demo.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final JwtService jwtService;
    private final HttpServletRequest httpRequest;

    @Override
    public ProductDisplayDto addProduct(ProductRequestDto request) {
        String merchantId = getMerchantIdFromToken();
        String normalizedName = request.getName().trim().toLowerCase().replaceAll("[^a-z0-9]", "");

        // 1. Check if Parent Product exists
        Optional<Product> existingProductOpt = productRepository.findByNormalizedNameAndBrandIgnoreCase(
                normalizedName, request.getBrand()
        );

        Product product;
        if (existingProductOpt.isPresent()) {
            product = existingProductOpt.get();
            // Note: If you want to merge new categories into existing ones, add logic here.
        } else {
            // Create New Parent Product
            product = Product.builder()
                    .productId(sequenceGeneratorService.generateSequence(Product.SEQUENCE_NAME))
                    .normalizedName(normalizedName)
                    .name(request.getName())
                    .brand(request.getBrand())
                    .description(request.getDescription())
                    .categories(request.getCategories()) // ✅ List<String>
                    .specs(request.getSpecs())           // ✅ Global Specs
                    .isActive(true)
                    .variants(new ArrayList<>())
                    .build();
        }

        // 2. Handle Variant
        ProductVariant targetVariant = findMatchingVariant(product, request.getAttributes());

        if (targetVariant == null) {
            // Create New Variant
            targetVariant = new ProductVariant();
            targetVariant.setVariantId(UUID.randomUUID().toString());
            targetVariant.setAttributes(request.getAttributes());

            // ✅ Assign Images to this specific variant (e.g., Black Phone Images)
            targetVariant.setImageUrls(request.getImageUrls());

            targetVariant.setOffers(new ArrayList<>());
            product.getVariants().add(targetVariant);
        }

        // 3. Update Merchant Offer
        targetVariant.getOffers().removeIf(offer -> offer.getMerchantId().equals(merchantId));

        MerchantOffer offer = new MerchantOffer(
                merchantId,
                "Merchant " + merchantId,
                request.getPrice(),
                request.getQuantity()
        );
        targetVariant.getOffers().add(offer);

        // 4. Save
        Product savedProduct = productRepository.save(product);

        return mapToDisplayDto(savedProduct, targetVariant);
    }

    @Override
    public List<ProductDisplayDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        List<ProductDisplayDto> displayList = new ArrayList<>();

        for (Product product : products) {
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    displayList.add(mapToDisplayDto(product, variant));
                }
            }
        }
        return displayList;
    }

    @Override
    public ProductDetailDto getProductDetails(Integer productId, String variantId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getVariantId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        List<MerchantOfferDto> sellerList = variant.getOffers().stream()
                .map(offer -> MerchantOfferDto.builder()
                        .merchantId(offer.getMerchantId())
                        .merchantName(offer.getMerchantName())
                        .price(offer.getPrice())
                        .stock(offer.getStock())
                        .build())
                .collect(Collectors.toList());

        return ProductDetailDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .categories(product.getCategories()) // ✅ List<String>
                .specs(product.getSpecs())           // ✅ Global Specs
                .imageUrls(variant.getImageUrls())   // ✅ Return variant-specific images
                .variantId(variant.getVariantId())
                .attributes(variant.getAttributes())
                .sellers(sellerList)
                .build();
    }

    @Override
    public void updateInventory(Integer productId, String variantId, Double newPrice, Integer newStock) {
        String merchantId = getMerchantIdFromToken();

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getVariantId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        MerchantOffer offer = variant.getOffers().stream()
                .filter(o -> o.getMerchantId().equals(merchantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("You do not have an active offer for this product."));

        if (newPrice != null) offer.setPrice(newPrice);
        if (newStock != null) offer.setStock(newStock);

        productRepository.save(product);
    }

    @Override
    public void removeInventory(Integer productId, String variantId) {
        String merchantId = getMerchantIdFromToken();

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getVariantId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        boolean removed = variant.getOffers().removeIf(o -> o.getMerchantId().equals(merchantId));

        if (!removed) {
            throw new RuntimeException("Offer not found or you are not authorized to delete it.");
        }

        if (variant.getOffers().isEmpty()) {
            product.getVariants().remove(variant);
        }

        productRepository.save(product);
    }

    // --- Helpers ---

    private String getMerchantIdFromToken() {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtService.extractUsername(authHeader.substring(7));
        }
        throw new RuntimeException("Unauthorized");
    }

    private ProductVariant findMatchingVariant(Product product, Map<String, String> attributes) {
        if (product.getVariants() == null) return null;

        for (ProductVariant variant : product.getVariants()) {
            if (variant.getAttributes().equals(attributes)) {
                return variant;
            }
        }
        return null;
    }

    private ProductDisplayDto mapToDisplayDto(Product product, ProductVariant variant) {
        Double minPrice = variant.getOffers().stream()
                .map(MerchantOffer::getPrice)
                .min(Double::compare)
                .orElse(0.0);

        int totalStock = variant.getOffers().stream()
                .mapToInt(MerchantOffer::getStock)
                .sum();

        // Logic: Use variant-specific image if available, else null/placeholder
        String thumbnail = (variant.getImageUrls() != null && !variant.getImageUrls().isEmpty())
                ? variant.getImageUrls().get(0)
                : null;

        return ProductDisplayDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .imageUrl(thumbnail) // ✅ Shows specific variant thumbnail
                .categories(product.getCategories()) // ✅ List<String>
                .attributes(variant.getAttributes())
                .lowestPrice(minPrice)
                .totalMerchants(variant.getOffers().size())
                .inStock(totalStock > 0)
                .variantId(variant.getVariantId())
                .build();
    }
}