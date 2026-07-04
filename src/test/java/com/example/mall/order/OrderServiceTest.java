package com.example.mall.order;

import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();

        productRepository = new ProductRepository();
        productRepository.deleteById(1L);
        productRepository.deleteById(2L);

        Product product1 = new Product(1L, "Test Product 1", "SKU-001", 100, 10000);
        Product product2 = new Product(2L, "Test Product 2", "SKU-002", 50, 20000);
        productRepository.save(product1);
        productRepository.save(product2);

        orderService = new OrderService(orderRepository, productRepository);
    }

    @Test
    void testListAllWithProducts() {
        Order order = new Order(null, "ORDER-TEST-001", Arrays.asList(1L, 2L), 30000, LocalDateTime.now());
        orderService.create(order);

        List<OrderWithProductsDTO> dtos = orderService.listAllWithProducts();
        assertFalse(dtos.isEmpty());
        
        OrderWithProductsDTO dto = dtos.stream()
                .filter(d -> "ORDER-TEST-001".equals(d.getOrderSn()))
                .findFirst()
                .orElse(null);
        assertNotNull(dto);
        assertEquals(2, dto.getProducts().size());
        assertEquals("Test Product 1", dto.getProducts().get(0).getName());
        assertEquals("Test Product 2", dto.getProducts().get(1).getName());
    }

    @Test
    void testFindByIdWithProducts() {
        Order order = new Order(null, "ORDER-TEST-002", Arrays.asList(1L), 10000, LocalDateTime.now());
        Order created = orderService.create(order);

        Optional<OrderWithProductsDTO> optionalDto = orderService.findByIdWithProducts(created.getId());
        assertTrue(optionalDto.isPresent());
        
        OrderWithProductsDTO dto = optionalDto.get();
        assertEquals(1, dto.getProducts().size());
        assertEquals("Test Product 1", dto.getProducts().get(0).getName());
    }

    @Test
    void testProductNotFoundShowsPlaceholder() {
        Order order = new Order(null, "ORDER-TEST-003", Arrays.asList(999L), 10000, LocalDateTime.now());
        orderService.create(order);

        List<OrderWithProductsDTO> dtos = orderService.listAllWithProducts();
        assertFalse(dtos.isEmpty());
        
        OrderWithProductsDTO dto = dtos.stream()
                .filter(d -> "ORDER-TEST-003".equals(d.getOrderSn()))
                .findFirst()
                .orElse(null);
        assertNotNull(dto);
        assertEquals("商品已下架或不存在", dto.getProducts().get(0).getName());
    }
}
