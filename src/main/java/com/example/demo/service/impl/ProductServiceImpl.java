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

        Optional<Product> existingProductOpt = productRepository.findByNormalizedNameAndBrandIgnoreCase(
                normalizedName, request.getBrand()
        );

        Product product;
        if (existingProductOpt.isPresent()) {
            product = existingProductOpt.get();
        } else {
            product = Product.builder()
                    .productId(sequenceGeneratorService.generateSequence(Product.SEQUENCE_NAME))
                    .normalizedName(normalizedName)
                    .name(request.getName())
                    .brand(request.getBrand())
                    .description(request.getDescription())
                    .categories(request.getCategories())
                    .specs(request.getSpecs())
                    .isActive(true)
                    .variants(new ArrayList<>())
                    .build();
        }

        ProductVariant targetVariant = findMatchingVariant(product, request.getAttributes());

        if (targetVariant == null) {
            targetVariant = new ProductVariant();
            targetVariant.setVariantId(UUID.randomUUID().toString());
            targetVariant.setAttributes(request.getAttributes());
            targetVariant.setImageUrls(request.getImageUrls());
            targetVariant.setOffers(new ArrayList<>());
            product.getVariants().add(targetVariant);
        }

        targetVariant.getOffers().removeIf(offer -> offer.getMerchantId().equals(merchantId));

        MerchantOffer offer = new MerchantOffer(
                merchantId,
                "Merchant " + merchantId,
                request.getPrice(),
                request.getQuantity()
        );
        targetVariant.getOffers().add(offer);

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

    // ✅ FIXED: Logic to filter by Merchant ID correctly
    @Override
    public List<ProductDisplayDto> getMerchantProducts() {
        String merchantId = getMerchantIdFromToken();
        List<Product> products = productRepository.findAll();
        List<ProductDisplayDto> merchantList = new ArrayList<>();

        for (Product product : products) {
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {

                    // 🔍 Step 1: Find YOUR specific offer inside this variant
                    Optional<MerchantOffer> myOfferOpt = variant.getOffers().stream()
                            .filter(offer -> offer.getMerchantId().equals(merchantId))
                            .findFirst();

                    // 🔍 Step 2: If found, use YOUR data (Price/Stock)
                    if (myOfferOpt.isPresent()) {
                        MerchantOffer myOffer = myOfferOpt.get();

                        String thumbnail = (variant.getImageUrls() != null && !variant.getImageUrls().isEmpty())
                                ? variant.getImageUrls().get(0)
                                : null;

                        ProductDisplayDto dto = ProductDisplayDto.builder()
                                .productId(product.getProductId())
                                .name(product.getName())
                                .brand(product.getBrand())
                                .description(product.getDescription())
                                .imageUrl(thumbnail)
                                .categories(product.getCategories())
                                .attributes(variant.getAttributes())
                                .variantId(variant.getVariantId())

                                // ✅ FIX: Use YOUR price, not the lowest on market
                                .lowestPrice(myOffer.getPrice())

                                // ✅ FIX: Use YOUR stock count
                                .totalMerchants(myOffer.getStock()) // We pass stock count here for the UI

                                // ✅ FIX: Calculate 'In Stock' based on YOUR inventory
                                .inStock(myOffer.getStock() > 0)
                                .build();

                        merchantList.add(dto);
                    }
                }
            }
        }
        return merchantList;
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
                .categories(product.getCategories())
                .specs(product.getSpecs())
                .imageUrls(variant.getImageUrls())
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

    // This method is for the PUBLIC page (Aggregates all sellers)
    private ProductDisplayDto mapToDisplayDto(Product product, ProductVariant variant) {
        Double minPrice = variant.getOffers().stream()
                .map(MerchantOffer::getPrice)
                .min(Double::compare)
                .orElse(0.0);

        int totalStock = variant.getOffers().stream()
                .mapToInt(MerchantOffer::getStock)
                .sum();

        String thumbnail = (variant.getImageUrls() != null && !variant.getImageUrls().isEmpty())
                ? variant.getImageUrls().get(0)
                : null;

        return ProductDisplayDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .imageUrl(thumbnail)
                .categories(product.getCategories())
                .attributes(variant.getAttributes())
                .lowestPrice(minPrice)
                .totalMerchants(variant.getOffers().size())
                .inStock(totalStock > 0)
                .variantId(variant.getVariantId())
                .build();
    }
}