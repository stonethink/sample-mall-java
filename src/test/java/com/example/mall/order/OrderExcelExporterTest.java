package com.example.mall.order;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class OrderExcelExporterTest {

    private final OrderExcelExporter exporter = new OrderExcelExporter();

    @Test
    void shouldExportEmptyList() throws IOException {
        byte[] result = exporter.exportToByteArray(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.length > 0, "Excel file should not be empty even with no data");
    }

    @Test
    void shouldExportSingleOrder() throws IOException {
        ProductInfo product = new ProductInfo();
        product.setId(1L);
        product.setName("Test Product");

        OrderWithProductsDTO order = new OrderWithProductsDTO();
        order.setId(1L);
        order.setOrderSn("ORDER-001");
        order.setProducts(Arrays.asList(product));
        order.setProductIds(Arrays.asList(1L));
        order.setTotalAmount(19900);
        order.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        order.setStatus(OrderStatus.PAID);
        order.setUserId(1L);

        byte[] result = exporter.exportToByteArray(Arrays.asList(order));
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void shouldHandleNullFields() throws IOException {
        OrderWithProductsDTO order = new OrderWithProductsDTO();
        byte[] result = exporter.exportToByteArray(Arrays.asList(order));
        assertNotNull(result);
        assertTrue(result.length > 0, "Should handle null fields without exception");
    }
}