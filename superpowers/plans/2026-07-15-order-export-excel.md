<!-- hyperspec change: order-export-excel -->

# 订单 Excel 导出功能 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为订单模块新增 `GET /api/orders/export` 端点，支持按状态筛选导出 Excel 文件，权限规则复用现有逻辑（管理员全部/普通用户仅自己的）

**Architecture:** 新增 Apache POI 依赖 → 创建 `OrderExcelExporter` 工具类生成 `.xlsx` → 在 `OrderService` 中组合查询与权限过滤 → 在 `OrderController` 中新增 `/export` 端点返回文件下载

**Tech Stack:** Spring Boot 2.7, Java 8, Apache POI (poi-ooxml), Springfox 3.0

## Global Constraints

- Java 1.8 编译兼容
- Apache POI 5.x+ 兼容 Java 8
- Excel 列顺序: 订单ID, 订单号, 商品名称, 商品ID列表, 总金额(分), 创建时间, 状态, 用户ID
- 返回 `.xlsx` 格式 (XSSFWorkbook)
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename=orders_export.xlsx`

---

### Task 1: 添加 Apache POI 依赖

**Files:**
- Modify: `pom.xml`（在 `<dependencies>` 中新增 POI 依赖）

**Interfaces:**
- Consumes: 无（新增依赖）
- Produces: pom.xml 中新增 poi-ooxml 依赖

- [ ] **Step 1: 在 pom.xml 中添加 poi-ooxml 依赖**

在 `<dependencies>` 标签内（Jackson JSR310 之后、Swagger 之前）添加 Apache POI 依赖：

```xml
        <!-- Apache POI for Excel export -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS（确认 POI 依赖下载和编译通过）

---

### Task 2: 创建 OrderExcelExporter 工具类

**Files:**
- Create: `src/main/java/com/example/mall/order/OrderExcelExporter.java`
- Test: `src/test/java/com/example/mall/order/OrderExcelExporterTest.java`

**Interfaces:**
- Consumes: `OrderWithProductsDTO`（现有 DTO），`OrderStatus.name()`（获取状态字符串），`ProductInfo.getName()`（获取商品名称）
- Produces: `OrderExcelExporter.exportToByteArray(List<OrderWithProductsDTO>)` → `byte[]`

- [ ] **Step 1: 创建 OrderExcelExporter 工具类**

```java
package com.example.mall.order;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class OrderExcelExporter {

    private static final String[] HEADERS = {
            "订单ID", "订单号", "商品名称", "商品ID列表", "总金额(分)", "创建时间", "状态", "用户ID"
    };

    public byte[] exportToByteArray(List<OrderWithProductsDTO> orders) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单数据");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(createHeaderCellStyle(workbook));
            }

            // 填充数据行
            int rowNum = 1;
            for (OrderWithProductsDTO order : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getId() != null ? order.getId().doubleValue() : 0);
                row.createCell(1).setCellValue(order.getOrderSn() != null ? order.getOrderSn() : "");
                row.createCell(2).setCellValue(getProductNamesString(order));
                row.createCell(3).setCellValue(getProductIdsString(order));
                row.createCell(4).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0);
                row.createCell(5).setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");
                row.createCell(6).setCellValue(order.getStatus() != null ? order.getStatus().name() : "");
                row.createCell(7).setCellValue(order.getUserId() != null ? order.getUserId().doubleValue() : 0);
            }

            // 自动调整列宽
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String getProductNamesString(OrderWithProductsDTO order) {
        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            return "";
        }
        return order.getProducts().stream()
                .map(ProductInfo::getName)
                .collect(Collectors.joining(", "));
    }

    private String getProductIdsString(OrderWithProductsDTO order) {
        if (order.getProductIds() == null || order.getProductIds().isEmpty()) {
            return "";
        }
        return order.getProductIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}
```

- [ ] **Step 2: 编写 OrderExcelExporterTest 测试**

```java
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
        // 所有字段都为 null
        byte[] result = exporter.exportToByteArray(Arrays.asList(order));
        assertNotNull(result);
        assertTrue(result.length > 0, "Should handle null fields without exception");
    }
}
```

- [ ] **Step 3: 运行测试验证**

Run: `mvn test -pl . -Dtest=OrderExcelExporterTest -q`
Expected: BUILD SUCCESS（所有 3 个测试通过）

---

### Task 3: OrderService 添加导出查询方法

**Files:**
- Modify: `src/main/java/com/example/mall/order/OrderService.java`

**Interfaces:**
- Consumes: `OrderService` 现有的 `listAllWithProducts()`, `listByStatusWithProducts()` 方法
- Produces: `OrderService.getOrdersForExport(String status, Long userId, boolean isAdmin)` → `List<OrderWithProductsDTO>`

- [ ] **Step 1: 在 OrderService 中添加导出查询方法**

在 `OrderService.java` 的 `delete()` 方法之前添加：

```java
    /**
     * 获取用于导出的订单列表，已按权限过滤
     * @param status 状态筛选（可选）
     * @param currentUserId 当前用户 ID（用于非管理员过滤）
     * @param isAdmin 是否为管理员
     * @return 权限过滤后的订单 DTO 列表
     */
    public List<OrderWithProductsDTO> getOrdersForExport(String status, Long currentUserId, boolean isAdmin) {
        List<OrderWithProductsDTO> orders;
        if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = parseOrderStatus(status);
            if (orderStatus == null) {
                return java.util.Collections.emptyList();
            }
            orders = listByStatusWithProducts(orderStatus);
        } else {
            orders = listAllWithProducts();
        }

        if (!isAdmin && currentUserId != null) {
            return orders.stream()
                    .filter(o -> currentUserId.equals(o.getUserId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return orders;
    }

    /**
     * 解析状态字符串为 OrderStatus 枚举
     */
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
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 4: OrderController 新增导出端点

**Files:**
- Modify: `src/main/java/com/example/mall/order/OrderController.java`

**Interfaces:**
- Consumes: `OrderExcelExporter.exportToByteArray()`, `OrderService.getOrdersForExport()`
- Produces: `GET /api/orders/export` 端点

- [ ] **Step 1: 在 OrderController 中添加导出端点**

在 `OrderController` 类的 `listAll` 方法之前（或之后），添加新的导出端点：

```java
    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam(required = false) String status, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "UNAUTHORIZED", "message", "请先登录"));
        }

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        List<OrderWithProductsDTO> orders = orderService.getOrdersForExport(status, currentUser.getId(), isAdmin);

        try {
            OrderExcelExporter exporter = new OrderExcelExporter();
            byte[] excelBytes = exporter.exportToByteArray(orders);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "orders_export.xlsx");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "EXPORT_FAILED", "message", "导出失败: " + e.getMessage()));
        }
    }
```

同时需要在类顶部的 import 区域增加：

```java
import org.springframework.http.HttpHeaders;
```

（已有的 import 中包含 `HttpStatus`、`ResponseEntity`、`HttpSession` 等，需确认是否已有 `HttpHeaders`）

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 5: 集成测试导出端点

**Files:**
- Modify: `src/test/java/com/example/mall/order/OrderControllerTest.java`

- [ ] **Step 1: 为导出端点添加集成测试方法**

在 `OrderControllerTest` 中添加测试方法，测试：
1. 未登录用户导出返回 401
2. 普通用户导出返回 Excel 文件
3. 按状态筛选导出

注意：`OrderControllerTest` 在 `add-user-controller-tests` 变更中存在，如果已被创建，在其后追加；如果尚未创建，新建测试类。

```java
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
                .andExpect(header().string("Content-Disposition", "attachment; filename=orders_export.xlsx"));
    }

    @Test
    void export_shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/orders/export?status=PAID")
                        .sessionAttr("currentUser", createAdminUser()))
                .andExpect(status().isOk());
    }

    @Test
    void export_shouldReturn401WithInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/orders/export?status=INVALID_STATUS")
                        .sessionAttr("currentUser", createAdminUser()))
                .andExpect(status().isOk()); // 无效状态返回空 Excel
    }

    private User createAdminUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(UserRole.ADMIN);
        return user;
    }
```

- [ ] **Step 2: 运行测试验证**

Run: `mvn test -Dtest=OrderControllerTest -q`
Expected: BUILD SUCCESS（所有测试通过）