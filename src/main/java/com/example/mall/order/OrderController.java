package com.example.mall.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> listAll(@RequestParam(required = false) String status) {
        if (status != null) {
            // 验证状态值是否有效（空字符串也视为无效）
            OrderStatus orderStatus = parseOrderStatus(status);
            if (orderStatus == null) {
                return buildInvalidStatusResponse(status);
            }
            return ResponseEntity.ok(orderService.listByStatus(orderStatus));
        }
        return ResponseEntity.ok(orderService.listAll());
    }

    // 对齐 mall-admin：/order/list 查询订单列表
    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) String status) {
        return listAll(status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return orderService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        Order created = orderService.create(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody Order order) {
        Order updated = orderService.update(id, order);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        // 4.4 无效输入校验 - 检查 status 字段是否存在
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

        // 4.4 无效输入校验 - 检查 status 值是否合法
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
            // 4.3 订单不存在返回 404
            OrderStatusErrorResponse error = OrderStatusErrorResponse.builder()
                    .error("ORDER_NOT_FOUND")
                    .message(e.getMessage())
                    .orderId(id)
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (IllegalStateException e) {
            // 4.3 非法状态流转返回 400
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

    /**
     * 解析状态字符串为 OrderStatus 枚举
     * @param status 状态字符串
     * @return OrderStatus 枚举值，如果无效返回 null
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

    /**
     * 获取所有允许的状态值列表
     */
    private List<String> getAllowedStatusValues() {
        return Arrays.stream(OrderStatus.values())
                .map(OrderStatus::name)
                .collect(Collectors.toList());
    }

    /**
     * 构建无效状态值的错误响应
     */
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
