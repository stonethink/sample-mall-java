package com.example.mall.order;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> listAll() {
        return orderRepository.findAll();
    }

    public List<Order> listByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Order create(Order order) {
        // 强制设置订单状态为待付款，忽略客户端传入的状态
        order.setStatus(OrderStatus.PENDING_PAYMENT);
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
        // 注意：不更新 status 字段，保留 existing 的原有状态
        return orderRepository.save(existing);
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}
