package com.example.mall.order;

import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
import com.example.mall.user.User;
import com.example.mall.user.UserRole;
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

    @Test
    void export_shouldReturn401WhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/orders/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void export_shouldReturnExcelFile() throws Exception {
        mockMvc.perform(get("/api/orders/export")
                        .sessionAttr("currentUser", createAdminUser()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"orders_export.xlsx\""));
    }

    @Test
    void export_shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/orders/export?status=PAID")
                        .sessionAttr("currentUser", createAdminUser()))
                .andExpect(status().isOk());
    }

    @Test
    void export_shouldReturnOkWithInvalidStatus() throws Exception {
        // 无效状态返回空Excel（空列表导出）
        mockMvc.perform(get("/api/orders/export?status=INVALID_STATUS")
                        .sessionAttr("currentUser", createAdminUser()))
                .andExpect(status().isOk());
    }

    private User createAdminUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(UserRole.ADMIN);
        return user;
    }
}
