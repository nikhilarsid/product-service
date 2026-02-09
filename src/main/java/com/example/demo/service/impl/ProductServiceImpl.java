package com.example.demo.service.impl;

// Add these imports at the top
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository productRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final JwtService jwtService;
    private final HttpServletRequest httpRequest;
    private final MongoTemplate mongoTemplate; // Add to constructor

    @Override
    public ProductDisplayDto addProduct(ProductRequestDto request) {
        String merchantId = getMerchantIdFromToken();
        String normalizedName = request.getName().trim().toLowerCase().replaceAll("[^a-z0-9]", "");

        Optional<Product> existingProductOpt = productRepository.findByNormalizedNameAndBrandIgnoreCase(
                normalizedName, request.getBrand());

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
                    .usp(request.getUsp())
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
            if (product.getVariants() == null) {
                product.setVariants(new ArrayList<>());
            }
            product.getVariants().add(targetVariant);
        }

        targetVariant.getOffers().removeIf(offer -> offer.getMerchantId().equals(merchantId));

        MerchantOffer offer = new MerchantOffer(
                merchantId,
                "Merchant " + merchantId,
                request.getPrice(),
                request.getQuantity());
        targetVariant.getOffers().add(offer);

        Product savedProduct = productRepository.save(product);
        return mapToDisplayDto(savedProduct, targetVariant);
    }

    @Override
    public Page<ProductDisplayDto> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);

        List<ProductDisplayDto> displayList = productPage.getContent().stream()
                .flatMap(product -> product.getVariants().stream()
                        .map(variant -> mapToDisplayDto(product, variant)))
                .collect(Collectors.toList());

        return new PageImpl<>(displayList, pageable, productPage.getTotalElements());
    }

    @Override
    public List<ProductDisplayDto> getMerchantProducts() {
        String merchantId = getMerchantIdFromToken();
        List<Product> products = productRepository.findAll();
        List<ProductDisplayDto> merchantList = new ArrayList<>();

        for (Product product : products) {
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    Optional<MerchantOffer> myOfferOpt = variant.getOffers().stream()
                            .filter(offer -> offer.getMerchantId().equals(merchantId))
                            .findFirst();

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
                                .lowestPrice(myOffer.getPrice())
                                .totalMerchants(myOffer.getStock())
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
                .usp(product.getUsp())
                .imageUrls(variant.getImageUrls())
                .variantId(variant.getVariantId())
                .attributes(variant.getAttributes())
                .sellers(sellerList)
                .build();
    }
    @Override
    public void populateRandomUSPs() {
        List<Product> products = productRepository.findAll();
        List<String> poolOfUSPs = Arrays.asList(
                "Premium Build Quality", "Eco-Friendly Materials", "Best in Class Warranty",
                "Award Winning Design", "Fast Charging Support", "Water Resistant",
                "Ultra Lightweight", "Limited Edition", "Energy Efficient"
        );

        for (Product product : products) {
            if (product.getUsp() == null || product.getUsp().isEmpty()) {
                List<String> randomUSPs = new ArrayList<>(poolOfUSPs);
                Collections.shuffle(randomUSPs);
                product.setUsp(randomUSPs.subList(0, 3));
                productRepository.save(product); // Persist to MongoDB
            }
        }
    }
    @Override
    public void updateInventory(Integer productId, String variantId, Double newPrice, Integer newStock) {
        String merchantId = getMerchantIdFromToken(); // Uses Token

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

        if (newPrice != null)
            offer.setPrice(newPrice);
        if (newStock != null)
            offer.setStock(newStock);

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

    // ✅ NEW: Logic to reduce stock for a specific merchant during order placement
    @Override
    public void reduceStock(Integer productId, String variantId, String merchantId, Integer quantity) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getVariantId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        // Find the specific merchant's offer
        MerchantOffer offer = variant.getOffers().stream()
                .filter(o -> o.getMerchantId().equals(merchantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Merchant " + merchantId + " does not sell this variant."));

        // Check stock levels
        if (offer.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock for Merchant " + merchantId);
        }

        // Update stock
        offer.setStock(offer.getStock() - quantity);

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
        if (product.getVariants() == null)
            return null;
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
                .usp(product.getUsp())
                .variantId(variant.getVariantId())
                .build();
    }

    @Override
    public List<ProductDisplayDto> searchProducts(String query) {
        log.info("Initiating fuzzy search for query: {}", query);
        MongoCollection<Document> collection = mongoTemplate.getCollection("products");

        List<Document> pipeline = Arrays.asList(
                new Document("$search", new Document("index", "default")
                        .append("text", new Document("query", query)
                                .append("path", Arrays.asList("name", "description", "brand", "categories"))
                                .append("fuzzy", new Document("maxEdits", 1)))),
                new Document("$limit", 20));

        return executeSearchPipeline(collection, pipeline, "Search");
    }

    @Override
    public List<ProductDisplayDto> suggestProducts(String query) {
        log.info("Initiating auto-suggest for query: {}", query);
        MongoCollection<Document> collection = mongoTemplate.getCollection("products");

        List<Document> pipeline = Arrays.asList(
                new Document("$search", new Document("index", "default")
                        .append("autocomplete", new Document("query", query)
                                .append("path", "name")
                                .append("fuzzy", new Document("maxEdits", 1)))),
                new Document("$limit", 5));

        return executeSearchPipeline(collection, pipeline, "Suggest");
    }

    private List<ProductDisplayDto> executeSearchPipeline(MongoCollection<Document> collection, List<Document> pipeline,
            String type) {
        List<ProductDisplayDto> results = new ArrayList<>();

        log.info("{} Pipeline: {}", type, pipeline.toString()); // Logs the exact JSON sent to Mongo

        try {
            AggregateIterable<Document> aggregationResults = collection.aggregate(pipeline);

            int count = 0;
            for (Document doc : aggregationResults) {
                count++;
                log.debug("Found Document ID: {}", doc.get("_id"));

                Product product = mongoTemplate.getConverter().read(Product.class, doc);
                if (product != null && product.getVariants() != null && !product.getVariants().isEmpty()) {
                    results.add(mapToDisplayDto(product, product.getVariants().get(0)));
                } else {
                    log.warn("Document found but has no variants or failed to map: {}", doc.get("_id"));
                }
            }

            log.info("{} finished. Found {} raw documents, returned {} DTOs.", type, count, results.size());
        } catch (Exception e) {
            log.error("Error executing Atlas Search pipeline: ", e);
        }

        return results;
    }
}