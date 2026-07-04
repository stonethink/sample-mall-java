package com.example.mall.order;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductInfoTest {
    @Test
    void testProductInfoCreation() {
        ProductInfo info = new ProductInfo();
        info.setId(1L);
        info.setName("Test Product");
        
        assertEquals(1L, info.getId());
        assertEquals("Test Product", info.getName());
    }
}
