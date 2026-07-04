package com.example.mall.order;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class OrderWithProductsDTOTest {
    @Test
    void testFromOrder() {
        Order order = new Order(1L, "ORDER-001", Arrays.asList(1L, 2L), 10000, LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUserId(1L);

        ProductInfo product1 = new ProductInfo();
        product1.setId(1L);
        product1.setName("Product 1");
        ProductInfo product2 = new ProductInfo();
        product2.setId(2L);
        product2.setName("Product 2");

        OrderWithProductsDTO dto = OrderWithProductsDTO.fromOrder(order, Arrays.asList(product1, product2));

        assertEquals(1L, dto.getId());
        assertEquals("ORDER-001", dto.getOrderSn());
        assertEquals(2, dto.getProducts().size());
        assertEquals("Product 1", dto.getProducts().get(0).getName());
        assertEquals("Product 2", dto.getProducts().get(1).getName());
        assertEquals(OrderStatus.PENDING_PAYMENT, dto.getStatus());
        assertEquals(1L, dto.getUserId());
    }
}
