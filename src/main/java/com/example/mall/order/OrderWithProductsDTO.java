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
