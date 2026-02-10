package com.example.demo.service;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.ProductDetailDto;
import com.example.demo.dto.response.ProductDisplayDto;
import com.example.demo.entity.MerchantOffer;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductVariant;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.impl.ProductServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SequenceGeneratorService sequenceGeneratorService;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductRequestDto productRequestDto;
    private Product product;
    private ProductVariant variant;
    private MerchantOffer offer;

    private final String MERCHANT_ID = "test-merchant-id";
    private final String VARIANT_ID = "test-variant-id";
    private final Integer PRODUCT_ID = 101;

    @BeforeEach
    void setUp() {
        // Setup DTO
        productRequestDto = new ProductRequestDto();
        productRequestDto.setName("Test Product");
        productRequestDto.setBrand("Test Brand");
        productRequestDto.setDescription("Test Description");
        productRequestDto.setCategories(List.of("Electronics"));
        productRequestDto.setPrice(100.0);
        productRequestDto.setQuantity(10);
        productRequestDto.setAttributes(Map.of("Color", "Black"));
        productRequestDto.setImageUrls(List.of("http://image.url"));
        productRequestDto.setSpecs(Map.of("Weight", "1kg"));

        // Setup Entity
        offer = new MerchantOffer(MERCHANT_ID, "Merchant " + MERCHANT_ID, 100.0, 10);
        variant = new ProductVariant();
        variant.setVariantId(VARIANT_ID);
        variant.setAttributes(Map.of("Color", "Black"));
        variant.setOffers(new ArrayList<>(List.of(offer)));
        variant.setImageUrls(List.of("http://image.url"));

        product = Product.builder()
                .productId(PRODUCT_ID)
                .name("Test Product")
                .brand("Test Brand")
                .normalizedName("test product")
                .isActive(true)
                .variants(new ArrayList<>(List.of(variant)))
                .build();
    }

    // ==========================================
    // 1. Add Product Tests
    // ==========================================

    @Test
    @DisplayName("Add Product - Success - New Product")
    void addProduct_NewProduct_Success() {
        // Mock Authentication
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn(MERCHANT_ID);

        // Mock Sequence Generator
        when(sequenceGeneratorService.generateSequence(Product.SEQUENCE_NAME)).thenReturn(PRODUCT_ID);

        // Mock Repository
        when(productRepository.findByNormalizedNameAndBrandIgnoreCase(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDisplayDto result = productService.addProduct(productRequestDto);

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        assertEquals(101, result.getProductId());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Add Product - Success - Existing Product, New Variant")
    void addProduct_ExistingProduct_NewVariant_Success() {
        // Mock Authentication
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn(MERCHANT_ID);

        // Existing product but different attributes
        productRequestDto.setAttributes(Map.of("Color", "Red"));

        when(productRepository.findByNormalizedNameAndBrandIgnoreCase(anyString(), anyString()))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDisplayDto result = productService.addProduct(productRequestDto);

        assertNotNull(result);
        assertEquals(2, product.getVariants().size()); // Should have 2 variants now
        verify(productRepository, times(1)).save(any(Product.class));
    }

    // ==========================================
    // 2. Get All Products Tests (FIXED)
    // ==========================================

    @Test
    @DisplayName("Get All Products - Success")
    void getAllProducts_Success() {
        // ✅ FIX: Added Pageable support
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> mockPage = new PageImpl<>(List.of(product));

        when(productRepository.findAll(pageable)).thenReturn(mockPage);

        Page<ProductDisplayDto> result = productService.getAllProducts(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(PRODUCT_ID, result.getContent().get(0).getProductId());
    }

    // ==========================================
    // 3. Get Merchant Products Tests
    // ==========================================

    @Test
    @DisplayName("Get Merchant Products - Success")
    void getMerchantProducts_Success() {
        // Mock Authentication
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn(MERCHANT_ID);

        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductDisplayDto> result = productService.getMerchantProducts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PRODUCT_ID, result.get(0).getProductId());
    }

    @Test
    @DisplayName("Get Merchant Products - Unauthorized Exception")
    void getMerchantProducts_Unauthorized() {
        when(httpRequest.getHeader("Authorization")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.getMerchantProducts();
        });

        assertEquals("Unauthorized", exception.getMessage());
    }

    // ==========================================
    // 4. Get Product Details Tests
    // ==========================================

    @Test
    @DisplayName("Get Product Details - Success")
    void getProductDetails_Success() {
        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductDetailDto result = productService.getProductDetails(PRODUCT_ID, VARIANT_ID);

        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.getProductId());
        assertEquals(VARIANT_ID, result.getVariantId());
    }

    @Test
    @DisplayName("Get Product Details - Product Not Found")
    void getProductDetails_ProductNotFound() {
        when(productRepository.findByProductId(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductDetails(999, VARIANT_ID);
        });
    }

    @Test
    @DisplayName("Get Product Details - Variant Not Found")
    void getProductDetails_VariantNotFound() {
        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductDetails(PRODUCT_ID, "non-existent-variant");
        });
    }

    // ==========================================
    // 5. Update Inventory Tests
    // ==========================================

    @Test
    @DisplayName("Update Inventory - Success")
    void updateInventory_Success() {
        // Mock Authentication
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn(MERCHANT_ID);

        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.updateInventory(PRODUCT_ID, VARIANT_ID, 150.0, 50);

        MerchantOffer updatedOffer = product.getVariants().get(0).getOffers().get(0);
        assertEquals(150.0, updatedOffer.getPrice());
        assertEquals(50, updatedOffer.getStock());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Update Inventory - Unauthorized (Offer Not Found for Merchant)")
    void updateInventory_Unauthorized() {
        // Mock Authentication for DIFFERENT merchant
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("other-merchant");

        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.updateInventory(PRODUCT_ID, VARIANT_ID, 150.0, 50);
        });

        assertEquals("You do not have an active offer for this product.", exception.getMessage());
    }

    // ==========================================
    // 6. Remove Inventory Tests
    // ==========================================

    @Test
    @DisplayName("Remove Inventory - Success")
    void removeInventory_Success() {
        // Mock Authentication
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn(MERCHANT_ID);

        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.removeInventory(PRODUCT_ID, VARIANT_ID);

        // Should remove variant since it was the only offer
        assertTrue(product.getVariants().isEmpty());
        verify(productRepository, times(1)).save(product);
    }

    // ==========================================
    // 7. Reduce Stock Tests (Order Service)
    // ==========================================

    @Test
    @DisplayName("Reduce Stock - Success")
    void reduceStock_Success() {
        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.reduceStock(PRODUCT_ID, VARIANT_ID, MERCHANT_ID, 5);

        assertEquals(5, product.getVariants().get(0).getOffers().get(0).getStock());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Reduce Stock - Insufficient Stock")
    void reduceStock_InsufficientStock() {
        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.reduceStock(PRODUCT_ID, VARIANT_ID, MERCHANT_ID, 20); // Requesting 20, have 10
        });

        assertEquals("Insufficient stock for Merchant " + MERCHANT_ID, exception.getMessage());
    }

    // ==========================================
    // 8. Search & Suggest Tests
    // ==========================================

    @Test
    @DisplayName("Search Products - Pipeline Verification using MongoTemplate")
    void searchProducts_Success() {
        @SuppressWarnings("unchecked")
        var collection = mock(com.mongodb.client.MongoCollection.class);
        var converter = mock(MongoConverter.class);

        when(mongoTemplate.getCollection("products")).thenReturn(collection);
        when(mongoTemplate.getConverter()).thenReturn(converter);

        var mockResult = mock(com.mongodb.client.AggregateIterable.class);
        when(collection.aggregate(anyList())).thenReturn(mockResult);

        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCursor<Document> mongoCursor = mock(com.mongodb.client.MongoCursor.class);
        Document doc = new Document("_id", "123").append("name", "Test Product");

        when(mockResult.iterator()).thenReturn(mongoCursor);
        when(mongoCursor.hasNext()).thenReturn(true, false);
        when(mongoCursor.next()).thenReturn(doc);

        when(converter.read(eq(Product.class), any(Document.class))).thenReturn(product);

        List<ProductDisplayDto> results = productService.searchProducts("test");

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mongoTemplate, times(1)).getCollection("products");
    }

    @Test
    @DisplayName("Search Products - Exception Handling")
    void searchProducts_Exception() {
        @SuppressWarnings("unchecked")
        var collection = mock(com.mongodb.client.MongoCollection.class);
        when(mongoTemplate.getCollection("products")).thenReturn(collection);
        when(collection.aggregate(anyList())).thenThrow(new RuntimeException("Mongo Error"));

        List<ProductDisplayDto> results = productService.searchProducts("test");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Search Products - Document Found but Mapping Failed/No Variants")
    void searchProducts_MappingFailure() {
        @SuppressWarnings("unchecked")
        var collection = mock(com.mongodb.client.MongoCollection.class);
        var converter = mock(MongoConverter.class);
        when(mongoTemplate.getCollection("products")).thenReturn(collection);
        when(mongoTemplate.getConverter()).thenReturn(converter);

        var mockResult = mock(com.mongodb.client.AggregateIterable.class);
        when(collection.aggregate(anyList())).thenReturn(mockResult);

        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCursor<Document> mongoCursor = mock(com.mongodb.client.MongoCursor.class);
        Document doc1 = new Document("_id", "1");
        Document doc2 = new Document("_id", "2");

        when(mockResult.iterator()).thenReturn(mongoCursor);
        when(mongoCursor.hasNext()).thenReturn(true, true, false);
        when(mongoCursor.next()).thenReturn(doc1, doc2);

        when(converter.read(eq(Product.class), eq(doc1))).thenReturn(null);
        Product productNoVariants = Product.builder().productId(2).variants(new ArrayList<>()).build();
        when(converter.read(eq(Product.class), eq(doc2))).thenReturn(productNoVariants);

        List<ProductDisplayDto> results = productService.searchProducts("test");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Suggest Products - Success")
    void suggestProducts_Success() {
        @SuppressWarnings("unchecked")
        var collection = mock(com.mongodb.client.MongoCollection.class);
        var converter = mock(MongoConverter.class);
        when(mongoTemplate.getCollection("products")).thenReturn(collection);
        when(mongoTemplate.getConverter()).thenReturn(converter);

        var mockResult = mock(com.mongodb.client.AggregateIterable.class);
        when(collection.aggregate(anyList())).thenReturn(mockResult);

        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCursor<Document> mongoCursor = mock(com.mongodb.client.MongoCursor.class);
        Document doc = new Document("_id", "123").append("name", "Test Product");

        when(mockResult.iterator()).thenReturn(mongoCursor);
        when(mongoCursor.hasNext()).thenReturn(true, false);
        when(mongoCursor.next()).thenReturn(doc);

        when(converter.read(eq(Product.class), any(Document.class))).thenReturn(product);

        List<ProductDisplayDto> results = productService.suggestProducts("test");

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mongoTemplate, times(1)).getCollection("products");
    }

    // ==========================================
    // 9. Extra Edge Cases (Coverage)
    // ==========================================

    @Test
    @DisplayName("Get Merchant Id - Missing/Invalid Header")
    void getMerchantProducts_InvalidHeader() {
        when(httpRequest.getHeader("Authorization")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> productService.getMerchantProducts());

        when(httpRequest.getHeader("Authorization")).thenReturn("Basic 12345");
        assertThrows(RuntimeException.class, () -> productService.getMerchantProducts());
    }

    @Test
    @DisplayName("Add Product - Null Variants List in Existing Product")
    void addProduct_NullVariantsSafeCheck() {
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn(MERCHANT_ID);

        product.setVariants(null);

        when(productRepository.findByNormalizedNameAndBrandIgnoreCase(anyString(), anyString()))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDisplayDto result = productService.addProduct(productRequestDto);

        assertNotNull(result);
        assertNotNull(product.getVariants());
        assertEquals(1, product.getVariants().size());
    }

    @Test
    @DisplayName("Update Inventory - Partial Updates")
    void updateInventory_Partial() {
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn(MERCHANT_ID);

        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.updateInventory(PRODUCT_ID, VARIANT_ID, 200.0, null);
        assertEquals(200.0, product.getVariants().get(0).getOffers().get(0).getPrice());
        assertEquals(10, product.getVariants().get(0).getOffers().get(0).getStock());

        productService.updateInventory(PRODUCT_ID, VARIANT_ID, null, 20);
        assertEquals(200.0, product.getVariants().get(0).getOffers().get(0).getPrice());
        assertEquals(20, product.getVariants().get(0).getOffers().get(0).getStock());
    }

    @Test
    @DisplayName("Remove Inventory - Unauthorized / Offer Not Found")
    void removeInventory_Unauthorized() {
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("other-merchant");

        when(productRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.removeInventory(PRODUCT_ID, VARIANT_ID);
        });

        assertEquals("Offer not found or you are not authorized to delete it.", exception.getMessage());
    }

    // ==========================================
    // 10. Populate Random USPs Tests (ADDED for coverage)
    // ==========================================

    @Test
    @DisplayName("Populate Random USPs - Success")
    void populateRandomUSPs_Success() {
        product.setUsp(null);
        when(productRepository.findAll()).thenReturn(List.of(product));

        productService.populateRandomUSPs();

        assertNotNull(product.getUsp());
        assertEquals(3, product.getUsp().size());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Populate Random USPs - Skip if Already Present")
    void populateRandomUSPs_Skip() {
        product.setUsp(List.of("Existing USP"));
        when(productRepository.findAll()).thenReturn(List.of(product));

        productService.populateRandomUSPs();

        assertEquals(1, product.getUsp().size());
        assertEquals("Existing USP", product.getUsp().get(0));
        verify(productRepository, times(0)).save(any(Product.class));
    }

    // ==========================================
    // 11. Variant Matching Logic (ADDED for coverage)
    // ==========================================

    @Test
    @DisplayName("Add Product - Matches Existing Variant")
    void addProduct_MatchesExistingVariant() {
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("NEW_MERCHANT");

        when(productRepository.findByNormalizedNameAndBrandIgnoreCase(anyString(), anyString()))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.addProduct(productRequestDto);

        assertEquals(1, product.getVariants().size());
        assertEquals(2, product.getVariants().get(0).getOffers().size());
    }
}