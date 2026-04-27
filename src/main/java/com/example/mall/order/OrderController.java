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

        List<Order> orders;
        if (status != null) {
            OrderStatus orderStatus = parseOrderStatus(status);
            if (orderStatus == null) {
                return buildInvalidStatusResponse(status);
            }
            orders = orderService.listByStatus(orderStatus);
        } else {
            orders = orderService.listAll();
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
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return orderService.findById(id)
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

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
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
