package com.example.mall.product;

import com.example.mall.category.CategoryRepository;
import com.example.mall.category.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTest {
    private MockMvc mockMvc;
    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private CategoryService categoryService;
    private ProductService productService;
    private ProductController productController;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepository();
        categoryRepository = new CategoryRepository();
        categoryService = new CategoryService(categoryRepository, productRepository);
        productService = new ProductService(productRepository, categoryRepository, categoryService);
        productController = new ProductController(productService);
        
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();

        Product product1 = new Product(1L, "测试商品1", "SKU-001", 100, 10000, Arrays.asList("cat-001"));
        Product product2 = new Product(2L, "测试商品2", "SKU-002", 50, 20000, Arrays.asList("cat-002"));
        productRepository.save(product1);
        productRepository.save(product2);
    }

    @Test
    void testListAllReturnsProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void testGetByIdReturnsProduct() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("测试商品1"))
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void testGetByIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content("{\"name\":\"新商品\",\"sku\":\"SKU-003\",\"stock\":200,\"price\":30000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("新商品"))
                .andExpect(jsonPath("$.sku").value("SKU-003"));
    }

    @Test
    void testUpdateProduct() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .contentType("application/json")
                        .content("{\"name\":\"更新商品\",\"sku\":\"SKU-001-UPD\",\"stock\":150,\"price\":15000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新商品"))
                .andExpect(jsonPath("$.sku").value("SKU-001-UPD"));
    }

    @Test
    void testDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
