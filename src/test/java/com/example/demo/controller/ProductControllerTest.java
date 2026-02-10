package com.example.demo.controller;

import com.example.demo.dto.request.ProductRequestDto;
import com.example.demo.dto.response.ProductDetailDto;
import com.example.demo.dto.response.ProductDisplayDto;
import com.example.demo.service.ProductService;
import com.example.demo.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false) // <--- KEY FIX: Disables Security/CSRF filters
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    // We mock JwtService because @WebMvcTest might try to load SecurityConfig,
    // which likely depends on JwtService.
    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductRequestDto productRequestDto;
    private ProductDisplayDto productDisplayDto;
    private ProductDetailDto productDetailDto;

    @BeforeEach
    void setUp() {
        // Setup Request DTO
        productRequestDto = new ProductRequestDto();
        productRequestDto.setName("Test Product");
        productRequestDto.setBrand("Test Brand");
        productRequestDto.setPrice(100.0);
        productRequestDto.setQuantity(10);
        productRequestDto.setCategories(List.of("Electronics"));
        productRequestDto.setAttributes(Map.of("Color", "Black"));

        // Setup Response DTO
        productDisplayDto = ProductDisplayDto.builder()
                .productId(1)
                .name("Test Product")
                .lowestPrice(100.0)
                .build();

        productDetailDto = ProductDetailDto.builder()
                .productId(1)
                .name("Test Product")
                .variantId("v1")
                .build();
    }

    // ==========================================
    // 1. Add Product Test (POST)
    // ==========================================
    @Test
    @DisplayName("Add Product - Success (201 Created)")
    void addProduct_Success() throws Exception {
        when(productService.addProduct(any(ProductRequestDto.class))).thenReturn(productDisplayDto);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.message").value("Product listing updated successfully"));

        verify(productService, times(1)).addProduct(any(ProductRequestDto.class));
    }

    // ==========================================
    // 2. Get All Products Tests (GET)
    // ==========================================

    @Test
    @DisplayName("Get All Products - No Category (Default Pagination)")
    void getAllProducts_NoCategory_Success() throws Exception {
        Page<ProductDisplayDto> page = new PageImpl<>(List.of(productDisplayDto));
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].productId").value(1));

        verify(productService, times(1)).getAllProducts(any(Pageable.class));
        verify(productService, never()).getProductsByCategory(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Get All Products - With Category")
    void getAllProducts_WithCategory_Success() throws Exception {
        Page<ProductDisplayDto> page = new PageImpl<>(List.of(productDisplayDto));
        when(productService.getProductsByCategory(eq("Electronics"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "Electronics")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].productId").value(1));

        verify(productService, times(1)).getProductsByCategory(eq("Electronics"), any(Pageable.class));
        verify(productService, never()).getAllProducts(any(Pageable.class));
    }

    // ==========================================
    // 3. My Listings Test (GET)
    // ==========================================
    @Test
    @DisplayName("Get My Listings - Success")
    void getMyListings_Success() throws Exception {
        when(productService.getMerchantProducts()).thenReturn(List.of(productDisplayDto));

        mockMvc.perform(get("/api/v1/products/my-listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productId").value(1))
                .andExpect(jsonPath("$.message").value("Merchant listings fetched"));

        verify(productService, times(1)).getMerchantProducts();
    }

    // ==========================================
    // 4. Get Product Details Test (GET)
    // ==========================================
    @Test
    @DisplayName("Get Product Details - Success")
    void getProductDetails_Success() throws Exception {
        when(productService.getProductDetails(eq(1), eq("v1"))).thenReturn(productDetailDto);

        mockMvc.perform(get("/api/v1/products/{id}", 1)
                        .param("variantId", "v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.variantId").value("v1"));

        verify(productService, times(1)).getProductDetails(1, "v1");
    }

    // ==========================================
    // 5. Update Inventory Test (PUT)
    // ==========================================
    @Test
    @DisplayName("Update Inventory - Success")
    void updateInventory_Success() throws Exception {
        doNothing().when(productService).updateInventory(anyInt(), anyString(), anyDouble(), anyInt());

        mockMvc.perform(put("/api/v1/products/inventory/{id}", 1)
                        .param("variantId", "v1")
                        .param("price", "150.0")
                        .param("stock", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inventory updated successfully"));

        verify(productService, times(1)).updateInventory(1, "v1", 150.0, 50);
    }

    // ==========================================
    // 6. Remove Inventory Test (DELETE)
    // ==========================================
    @Test
    @DisplayName("Remove Inventory - Success")
    void removeInventory_Success() throws Exception {
        doNothing().when(productService).removeInventory(anyInt(), anyString());

        mockMvc.perform(delete("/api/v1/products/inventory/{id}", 1)
                        .param("variantId", "v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product offer removed successfully"));

        verify(productService, times(1)).removeInventory(1, "v1");
    }

    // ==========================================
    // 7. Search & Suggest Tests (GET)
    // ==========================================
    @Test
    @DisplayName("Search Products - Success")
    void search_Success() throws Exception {
        when(productService.searchProducts("iphone")).thenReturn(List.of(productDisplayDto));

        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", "iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Product"));

        verify(productService, times(1)).searchProducts("iphone");
    }

    @Test
    @DisplayName("Suggest Products - Success")
    void suggest_Success() throws Exception {
        when(productService.suggestProducts("mac")).thenReturn(List.of(productDisplayDto));

        mockMvc.perform(get("/api/v1/products/suggest")
                        .param("q", "mac"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Product"));

        verify(productService, times(1)).suggestProducts("mac");
    }

    // ==========================================
    // 8. Reduce Stock Test (PUT)
    // ==========================================
    @Test
    @DisplayName("Reduce Stock - Success")
    void reduceStock_Success() throws Exception {
        doNothing().when(productService).reduceStock(anyInt(), anyString(), anyString(), anyInt());

        mockMvc.perform(put("/api/v1/products/reduce-stock/{id}", 1)
                        .param("variantId", "v1")
                        .param("merchantId", "m1")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stock reduced successfully"));

        verify(productService, times(1)).reduceStock(1, "v1", "m1", 5);
    }

    // ==========================================
    // 9. Migrate USPs Test (POST)
    // ==========================================
    @Test
    @DisplayName("Migrate USPs - Success")
    void migrateUsps_Success() throws Exception {
        doNothing().when(productService).populateRandomUSPs();

        mockMvc.perform(post("/api/v1/products/migrate-usps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Random USPs assigned to existing products"));

        verify(productService, times(1)).populateRandomUSPs();
    }
}