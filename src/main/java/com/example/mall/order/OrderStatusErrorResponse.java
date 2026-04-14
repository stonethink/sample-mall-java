package com.example.mall.order;

import java.util.List;

/**
 * 订单状态操作错误响应类
 * 用于封装状态流转错误、无效状态值等结构化错误信息
 */
public class OrderStatusErrorResponse {

    private String error;
    private String message;
    private Long orderId;
    private String currentStatus;
    private String requestedStatus;
    private List<String> allowedTransitions;

    // 用于无效状态值错误的字段
    private String parameter;
    private String providedValue;
    private List<String> allowedValues;

    public OrderStatusErrorResponse() {
    }

    /**
     * 构造状态流转错误响应
     */
    public OrderStatusErrorResponse(String error, String message, Long orderId,
                                    String currentStatus, String requestedStatus,
                                    List<String> allowedTransitions) {
        this.error = error;
        this.message = message;
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
        this.allowedTransitions = allowedTransitions;
    }

    /**
     * 构造无效状态值错误响应
     */
    public OrderStatusErrorResponse(String error, String message, String parameter,
                                    String providedValue, List<String> allowedValues) {
        this.error = error;
        this.message = message;
        this.parameter = parameter;
        this.providedValue = providedValue;
        this.allowedValues = allowedValues;
    }

    /**
     * 构造简单错误响应
     */
    public OrderStatusErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    // Getters and Setters

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getRequestedStatus() {
        return requestedStatus;
    }

    public void setRequestedStatus(String requestedStatus) {
        this.requestedStatus = requestedStatus;
    }

    public List<String> getAllowedTransitions() {
        return allowedTransitions;
    }

    public void setAllowedTransitions(List<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getProvidedValue() {
        return providedValue;
    }

    public void setProvidedValue(String providedValue) {
        this.providedValue = providedValue;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    /**
     * Builder 模式，用于灵活构建错误响应
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String error;
        private String message;
        private Long orderId;
        private String currentStatus;
        private String requestedStatus;
        private List<String> allowedTransitions;
        private String parameter;
        private String providedValue;
        private List<String> allowedValues;

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder currentStatus(String currentStatus) {
            this.currentStatus = currentStatus;
            return this;
        }

        public Builder requestedStatus(String requestedStatus) {
            this.requestedStatus = requestedStatus;
            return this;
        }

        public Builder allowedTransitions(List<String> allowedTransitions) {
            this.allowedTransitions = allowedTransitions;
            return this;
        }

        public Builder parameter(String parameter) {
            this.parameter = parameter;
            return this;
        }

        public Builder providedValue(String providedValue) {
            this.providedValue = providedValue;
            return this;
        }

        public Builder allowedValues(List<String> allowedValues) {
            this.allowedValues = allowedValues;
            return this;
        }

        public OrderStatusErrorResponse build() {
            OrderStatusErrorResponse response = new OrderStatusErrorResponse();
            response.error = this.error;
            response.message = this.message;
            response.orderId = this.orderId;
            response.currentStatus = this.currentStatus;
            response.requestedStatus = this.requestedStatus;
            response.allowedTransitions = this.allowedTransitions;
            response.parameter = this.parameter;
            response.providedValue = this.providedValue;
            response.allowedValues = this.allowedValues;
            return response;
        }
    }
}
