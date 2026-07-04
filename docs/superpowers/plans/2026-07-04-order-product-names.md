# 订单列表商品显示名称 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在订单列表API中显示商品名称，将 `productIds` 列表转换为包含商品ID和名称的 `products` 对象列表。

**Architecture:** 采用DTO模式，创建 `OrderWithProductsDTO` 和 `ProductInfo` 类。OrderService 在查询订单时，从 ProductRepository 获取商品详情并转换为DTO返回。保持原始 Order 实体不变，仅在查询时进行数据 enrichment。

**Tech Stack:** Java 21, Spring Boot, Maven

## Global Constraints

- 商品ID不存在时显示占位名称："商品已下架或不存在"
- 保持原有 Order 实体不变（只读 enrichment）
- 遵循项目现有的 DTO 模式（参考 ProductWithCategoriesDTO）

---

## Task 1: 创建 ProductInfo 类

**Files:**
- Create: `src/main/java/com/example/mall/order/ProductInfo.java`

**Interfaces:**
- Consumes: 无
- Produces: `ProductInfo` 类，包含 `id`(Long) 和 `name`(String) 字段

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ProductInfoTest -q`
Expected: FAIL with "cannot find symbol: class ProductInfo"

- [ ] **Step 3: Write minimal implementation**

```java
package com.example.mall.order;

public class ProductInfo {
    private Long id;
    private String name;

    public ProductInfo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ProductInfoTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/mall/order/ProductInfo.java src/test/java/com/example/mall/order/ProductInfoTest.java
git commit -m "feat(order): 创建 ProductInfo 类"
```

---

## Task 2: 创建 OrderWithProductsDTO 类

**Files:**
- Create: `src/main/java/com/example/mall/order/OrderWithProductsDTO.java`
- Create: `src/test/java/com/example/mall/order/OrderWithProductsDTOTest.java`

**Interfaces:**
- Consumes: `ProductInfo`（Task 1）
- Produces: `OrderWithProductsDTO` 类，包含订单所有字段 + `List<ProductInfo> products`

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OrderWithProductsDTOTest -q`
Expected: FAIL with "cannot find symbol: class OrderWithProductsDTO"

- [ ] **Step 3: Write minimal implementation**

```java
package com.example.mall.order;

import java.time.LocalDateTime;
import java.util.List;

public class OrderWithProductsDTO {
    private Long id;
    private String orderSn;
    private List<ProductInfo> products;
    private Integer totalAmount;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private Long userId;

    public OrderWithProductsDTO() {
    }

    public static OrderWithProductsDTO fromOrder(Order order, List<ProductInfo> products) {
        OrderWithProductsDTO dto = new OrderWithProductsDTO();
        dto.setId(order.getId());
        dto.setOrderSn(order.getOrderSn());
        dto.setProducts(products);
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setStatus(order.getStatus());
        dto.setUserId(order.getUserId());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderSn() {
        return orderSn;
    }

    public void setOrderSn(String orderSn) {
        this.orderSn = orderSn;
    }

    public List<ProductInfo> getProducts() {
        return products;
    }

    public void setProducts(List<ProductInfo> products) {
        this.products = products;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=OrderWithProductsDTOTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/mall/order/OrderWithProductsDTO.java src/test/java/com/example/mall/order/OrderWithProductsDTOTest.java
git commit -m "feat(order): 创建 OrderWithProductsDTO 类"
```

---

## Task 3: 修改 OrderService 添加商品信息查询

**Files:**
- Modify: `src/main/java/com/example/mall/order/OrderService.java`
- Create: `src/test/java/com/example/mall/order/OrderServiceTest.java`

**Interfaces:**
- Consumes: `Order`, `OrderWithProductsDTO`, `ProductInfo`（Task 1, 2）, `ProductRepository`
- Produces: 新增方法 `listAllWithProducts()`, `listByStatusWithProducts()`, `findByIdWithProducts()`

- [ ] **Step 1: Write the failing test**

```java
package com.example.mall.order;

import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        orderRepository.deleteById(1L);
        orderRepository.deleteById(2L);

        productRepository = new ProductRepository();

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

        var dtos = orderService.listAllWithProducts();
        assertFalse(dtos.isEmpty());
        
        OrderWithProductsDTO dto = dtos.get(0);
        assertEquals(2, dto.getProducts().size());
        assertEquals("Test Product 1", dto.getProducts().get(0).getName());
        assertEquals("Test Product 2", dto.getProducts().get(1).getName());
    }

    @Test
    void testFindByIdWithProducts() {
        Order order = new Order(null, "ORDER-TEST-002", Arrays.asList(1L), 10000, LocalDateTime.now());
        Order created = orderService.create(order);

        var optionalDto = orderService.findByIdWithProducts(created.getId());
        assertTrue(optionalDto.isPresent());
        
        OrderWithProductsDTO dto = optionalDto.get();
        assertEquals(1, dto.getProducts().size());
        assertEquals("Test Product 1", dto.getProducts().get(0).getName());
    }

    @Test
    void testProductNotFoundShowsPlaceholder() {
        Order order = new Order(null, "ORDER-TEST-003", Arrays.asList(999L), 10000, LocalDateTime.now());
        orderService.create(order);

        var dtos = orderService.listAllWithProducts();
        assertFalse(dtos.isEmpty());
        
        OrderWithProductsDTO dto = dtos.stream()
                .filter(d -> "ORDER-TEST-003".equals(d.getOrderSn()))
                .findFirst()
                .orElse(null);
        assertNotNull(dto);
        assertEquals("商品已下架或不存在", dto.getProducts().get(0).getName());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OrderServiceTest -q`
Expected: FAIL with "constructor OrderService(OrderRepository, ProductRepository) is undefined"

- [ ] **Step 3: Write minimal implementation**

首先修改 OrderService，添加 ProductRepository 注入和新方法：

```java
package com.example.mall.order;

import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository) {
        this(orderRepository, null);
    }

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public List<Order> listAll() {
        return orderRepository.findAll();
    }

    public List<OrderWithProductsDTO> listAllWithProducts() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<Order> listByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public List<OrderWithProductsDTO> listByStatusWithProducts(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<OrderWithProductsDTO> findByIdWithProducts(Long id) {
        return orderRepository.findById(id)
                .map(this::convertToDTO);
    }

    private OrderWithProductsDTO convertToDTO(Order order) {
        List<ProductInfo> productInfos = new ArrayList<>();
        if (order.getProductIds() != null && productRepository != null) {
            for (Long productId : order.getProductIds()) {
                ProductInfo info = new ProductInfo();
                info.setId(productId);
                Optional<Product> product = productRepository.findById(productId);
                if (product.isPresent()) {
                    info.setName(product.get().getName());
                } else {
                    info.setName("商品已下架或不存在");
                }
                productInfos.add(info);
            }
        }
        return OrderWithProductsDTO.fromOrder(order, productInfos);
    }

    public Order create(Order order) {
        return create(order, null);
    }

    public Order create(Order order, Long userId) {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUserId(userId);
        return orderRepository.save(order);
    }

    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));

        OrderStatus currentStatus = order.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            String allowedTransitions = currentStatus.getAllowedTransitions().stream()
                    .map(OrderStatus::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s. Allowed transitions: [%s]",
                            currentStatus, newStatus, allowedTransitions));
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    public Order update(Long id, Order updated) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found, id=" + id));
        existing.setOrderSn(updated.getOrderSn());
        existing.setProductIds(updated.getProductIds());
        existing.setTotalAmount(updated.getTotalAmount());
        return orderRepository.save(existing);
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=OrderServiceTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/mall/order/OrderService.java src/test/java/com/example/mall/order/OrderServiceTest.java
git commit -m "feat(order): OrderService 添加商品信息查询方法"
```

---

## Task 4: 修改 OrderController 返回 DTO

**Files:**
- Modify: `src/main/java/com/example/mall/order/OrderController.java`

**Interfaces:**
- Consumes: `OrderWithProductsDTO`, `OrderService.listAllWithProducts()`, `OrderService.findByIdWithProducts()`（Task 3）
- Produces: API 返回 `OrderWithProductsDTO` 列表和单个 `OrderWithProductsDTO`

- [ ] **Step 1: Write the failing test**

```java
package com.example.mall.order;

import com.example.mall.product.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.hamcrest.Matchers.*;
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

        OrderService orderService = new OrderService(orderRepository, productRepository);
        OrderController orderController = new OrderController(orderService);

        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

        com.example.mall.product.Product product1 = new com.example.mall.product.Product(1L, "Test Product", "SKU-001", 100, 10000);
        productRepository.save(product1);

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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OrderControllerTest -q`
Expected: FAIL 因为 `/api/orders/1` 返回的是 `Order` 而不是 `OrderWithProductsDTO`，没有 `products` 字段

- [ ] **Step 3: Write minimal implementation**

修改 OrderController 的 `listAll()` 和 `getById()` 方法：

```java
package com.example.mall.order;

import com.example.mall.user.User;
import com.example.mall.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping({"/api/orders", "/order"})
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<?> listAll(@RequestParam(required = false) String status, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "请先登录"));
        }

        List<OrderWithProductsDTO> orders;
        if (status != null) {
            OrderStatus orderStatus = parseOrderStatus(status);
            if (orderStatus == null) {
                return buildInvalidStatusResponse(status);
            }
            orders = orderService.listByStatusWithProducts(orderStatus);
        } else {
            orders = orderService.listAllWithProducts();
        }

        if (currentUser.getRole() != UserRole.ADMIN) {
            orders = orders.stream()
                    .filter(o -> currentUser.getId().equals(o.getUserId()))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) String status, HttpSession session) {
        return listAll(status, session);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return orderService.findByIdWithProducts(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Order order, HttpSession session) {
        User currentUser = getCurrentUser(session);
        Long userId = currentUser != null ? currentUser.getId() : null;
        Order created = orderService.create(order, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody Order order) {
        Order updated = orderService.update(id, order);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        String statusStr = requestBody.get("status");
        if (statusStr == null) {
            OrderStatusErrorResponse error = OrderStatusErrorResponse.builder()
                    .error("INVALID_ORDER_STATUS")
                    .message("status field is required")
                    .parameter("status")
                    .providedValue(null)
                    .allowedValues(getAllowedStatusValues())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        OrderStatus newStatus = parseOrderStatus(statusStr);
        if (newStatus == null) {
            OrderStatusErrorResponse error = OrderStatusErrorResponse.builder()
                    .error("INVALID_ORDER_STATUS")
                    .message("Invalid status value: " + statusStr)
                    .parameter("status")
                    .providedValue(statusStr)
                    .allowedValues(getAllowedStatusValues())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            Order updated = orderService.updateStatus(id, newStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            OrderStatusErrorResponse error = OrderStatusErrorResponse.builder()
                    .error("ORDER_NOT_FOUND")
                    .message(e.getMessage())
                    .orderId(id)
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (IllegalStateException e) {
            Order order = orderService.findById(id).orElse(null);
            if (order != null) {
                List<String> allowedTransitions = order.getStatus().getAllowedTransitions().stream()
                        .map(OrderStatus::name)
                        .collect(Collectors.toList());
                OrderStatusErrorResponse error = OrderStatusErrorResponse.builder()
                        .error("INVALID_STATUS_TRANSITION")
                        .message(e.getMessage())
                        .orderId(id)
                        .currentStatus(order.getStatus().name())
                        .requestedStatus(newStatus.name())
                        .allowedTransitions(allowedTransitions)
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            OrderStatusErrorResponse error = OrderStatusErrorResponse.builder()
                    .error("INVALID_STATUS_TRANSITION")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    private OrderStatus parseOrderStatus(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> getAllowedStatusValues() {
        return Arrays.stream(OrderStatus.values())
                .map(OrderStatus::name)
                .collect(Collectors.toList());
    }

    private ResponseEntity<OrderStatusErrorResponse> buildInvalidStatusResponse(String providedValue) {
        OrderStatusErrorResponse error = OrderStatusErrorResponse.builder()
                .error("INVALID_ORDER_STATUS")
                .message("Invalid status value: " + providedValue)
                .parameter("status")
                .providedValue(providedValue)
                .allowedValues(getAllowedStatusValues())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=OrderControllerTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/mall/order/OrderController.java src/test/java/com/example/mall/order/OrderControllerTest.java
git commit -m "feat(order): OrderController 返回 OrderWithProductsDTO"
```

---

## Task 5: 集成测试和验证

**Files:**
- Run: 所有测试

**Interfaces:**
- Consumes: 所有之前的任务

- [ ] **Step 1: Run all tests**

Run: `mvn test -q`
Expected: All tests PASS

- [ ] **Step 2: 启动应用并验证 API**

Run: `mvn spring-boot:run`
然后访问: `http://localhost:8080/api/orders`
Expected: 返回的 JSON 包含 `products` 字段，每个商品包含 `id` 和 `name`

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(order): 完成订单列表商品显示名称功能"
```

---

## Self-Review

**1. Spec coverage:**
- ✅ 新增 ProductInfo 类（Task 1）
- ✅ 新增 OrderWithProductsDTO 类（Task 2）
- ✅ OrderService 添加商品信息查询（Task 3）
- ✅ OrderController 返回 DTO（Task 4）
- ✅ 商品不存在显示占位名称（Task 3, testProductNotFoundShowsPlaceholder）
- ✅ 集成测试（Task 5）

**2. Placeholder scan:**
- ✅ 无 "TBD", "TODO" 等占位符
- ✅ 所有步骤包含具体代码
- ✅ 所有测试包含具体断言

**3. Type consistency:**
- ✅ `ProductInfo` 在所有任务中使用一致
- ✅ `OrderWithProductsDTO` 在所有任务中使用一致
- ✅ 方法签名在 Task 3 和 Task 4 之间保持一致
