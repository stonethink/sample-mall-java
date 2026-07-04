package com.example.mall.order;

import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {
    private MockMvc mockMvc;
    private OrderRepository orderRepository;
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        productRepository = new ProductRepository();

        Product product1 = new Product(1L, "Test Product", "SKU-001", 100, 10000);
        productRepository.save(product1);

        OrderService orderService = new OrderService(orderRepository, productRepository);
        OrderController orderController = new OrderController(orderService);

        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

        Order order = new Order(null, "ORDER-CONTROLLER-001", Arrays.asList(1L), 10000, LocalDateTime.now());
        orderService.create(order);
    }

    @Test
    void testGetByIdReturnsDTOWithProducts() throws Exception {
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products[0].id").value(1))
                .andExpect(jsonPath("$.products[0].name").value("Test Product"));
    }
}
