package com.example.mall.order;

import com.example.mall.product.Product;
import com.example.mall.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
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
        // 强制设置订单状态为待付款，忽略客户端传入的状态
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
        // 注意：不更新 status 字段，保留 existing 的原有状态
        return orderRepository.save(existing);
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}
