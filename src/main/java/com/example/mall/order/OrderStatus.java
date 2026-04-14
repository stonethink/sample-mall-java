package com.example.mall.order;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 订单状态枚举
 * 定义订单生命周期中的各个状态及合法的状态转换路径
 */
public enum OrderStatus {
    PENDING_PAYMENT("待付款"),
    PAID("已付款"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查当前状态是否可以转换到目标状态
     *
     * 合法转换路径：
     * - PENDING_PAYMENT → PAID, CANCELLED
     * - PAID → SHIPPED, CANCELLED
     * - SHIPPED → COMPLETED
     * - COMPLETED → （无）
     * - CANCELLED → （无）
     *
     * @param target 目标状态
     * @return 如果转换合法返回 true，否则返回 false
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        // 同状态不允许转换
        if (this == target) {
            return false;
        }
        switch (this) {
            case PENDING_PAYMENT:
                return target == PAID || target == CANCELLED;
            case PAID:
                return target == SHIPPED || target == CANCELLED;
            case SHIPPED:
                return target == COMPLETED;
            case COMPLETED:
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }

    /**
     * 获取当前状态允许转换到的目标状态列表
     *
     * @return 允许转换的状态列表
     */
    public List<OrderStatus> getAllowedTransitions() {
        switch (this) {
            case PENDING_PAYMENT:
                return Arrays.asList(PAID, CANCELLED);
            case PAID:
                return Arrays.asList(SHIPPED, CANCELLED);
            case SHIPPED:
                return Collections.singletonList(COMPLETED);
            case COMPLETED:
            case CANCELLED:
                return Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }
}
