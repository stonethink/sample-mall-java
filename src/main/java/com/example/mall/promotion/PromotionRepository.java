package com.example.mall.promotion;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class PromotionRepository {

    private final ConcurrentHashMap<Long, Promotion> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<Promotion> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Promotion> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Promotion save(Promotion promotion) {
        if (promotion.getId() == null) {
            promotion.setId(idGenerator.getAndIncrement());
        }
        store.put(promotion.getId(), promotion);
        return promotion;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public List<Promotion> findByStatus(PromotionStatus status) {
        return store.values().stream()
                .filter(p -> p.getStatus() == status)
                .collect(Collectors.toList());
    }
}
