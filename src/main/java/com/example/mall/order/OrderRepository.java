package com.example.mall.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class OrderRepository {

    private final ConcurrentHashMap<Long, Order> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public OrderRepository() {
        // 从JSON文件加载示例数据
        loadOrdersFromJson();
    }
    
    private void loadOrdersFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("orders.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            List<Order> orders = objectMapper.readValue(inputStream, new TypeReference<List<Order>>(){});
                
            for (Order order : orders) {
                save(order);
            }
        } catch (IOException e) {
            System.err.println("Failed to load orders from JSON: " + e.getMessage());
            // 如果JSON加载失败,使用默认数据
            save(new Order(null, "ORDER-001", Arrays.asList(1L), 19900, LocalDateTime.now()));
            save(new Order(null, "ORDER-002", Arrays.asList(1L, 2L), 49800, LocalDateTime.now()));
        }
    }

    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<Order> findByStatus(OrderStatus status) {
        return store.values().stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList());
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(idGenerator.getAndIncrement());
        }
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        // 兼容性处理：对缺失 status 的订单默认设置为 PENDING_PAYMENT
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
        }
        store.put(order.getId(), order);
        return order;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}
