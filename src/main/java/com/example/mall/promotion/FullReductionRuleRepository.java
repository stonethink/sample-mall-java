package com.example.mall.promotion;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class FullReductionRuleRepository {

    private final ConcurrentHashMap<Long, FullReductionRule> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<FullReductionRule> findByPromotionId(Long promotionId) {
        return store.values().stream()
                .filter(r -> r.getPromotionId().equals(promotionId))
                .sorted((a, b) -> Integer.compare(a.getLevel(), b.getLevel()))
                .collect(Collectors.toList());
    }

    public Optional<FullReductionRule> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public FullReductionRule save(FullReductionRule rule) {
        if (rule.getId() == null) {
            rule.setId(idGenerator.getAndIncrement());
        }
        store.put(rule.getId(), rule);
        return rule;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public void deleteByPromotionId(Long promotionId) {
        List<Long> idsToDelete = store.values().stream()
                .filter(r -> r.getPromotionId().equals(promotionId))
                .map(FullReductionRule::getId)
                .collect(Collectors.toList());
        idsToDelete.forEach(store::remove);
    }
}
