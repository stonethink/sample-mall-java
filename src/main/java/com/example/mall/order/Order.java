package com.example.mall.order;

import java.time.LocalDateTime;
import java.util.List;

public class Order {

    private Long id;
    private String orderSn;
    private List<Long> productIds;
    private Integer totalAmount;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private Long userId;

    public Order() {
    }

    public Order(Long id, String orderSn, List<Long> productIds, Integer totalAmount, LocalDateTime createdAt) {
        this.id = id;
        this.orderSn = orderSn;
        this.productIds = productIds;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public Order(Long id, String orderSn, List<Long> productIds, Integer totalAmount, LocalDateTime createdAt, OrderStatus status) {
        this.id = id;
        this.orderSn = orderSn;
        this.productIds = productIds;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.status = status;
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

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
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
