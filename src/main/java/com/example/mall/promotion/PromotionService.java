package com.example.mall.promotion;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public Promotion create(Promotion promotion) {
        if (promotion.getName() == null || promotion.getName().length() < 2 || promotion.getName().length() > 50) {
            throw new IllegalArgumentException("活动名称长度必须在2-50个字符之间");
        }
        if (promotion.getStartTime() == null || promotion.getEndTime() == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        if (!promotion.getEndTime().isAfter(promotion.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        if (promotion.getPreheatTime() != null && !promotion.getPreheatTime().isBefore(promotion.getStartTime())) {
            throw new IllegalArgumentException("预热时间必须早于开始时间");
        }
        if (promotion.getType() == null) {
            promotion.setType(PromotionType.FULL_REDUCTION);
        }
        if (promotion.getEnabled() == null) {
            promotion.setEnabled(true);
        }
        if (promotion.getPriority() == null) {
            promotion.setPriority(0);
        }
        promotion.setCreatedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());
        return promotionRepository.save(promotion);
    }

    public Optional<Promotion> findById(Long id) {
        return promotionRepository.findById(id);
    }

    public List<Promotion> findAll() {
        return promotionRepository.findAll();
    }

    public Promotion update(Long id, Promotion updated) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在, id=" + id));
        if (updated.getName() != null) {
            if (updated.getName().length() < 2 || updated.getName().length() > 50) {
                throw new IllegalArgumentException("活动名称长度必须在2-50个字符之间");
            }
            existing.setName(updated.getName());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription());
        }
        if (updated.getPreheatTime() != null) {
            existing.setPreheatTime(updated.getPreheatTime());
        }
        if (updated.getStartTime() != null) {
            existing.setStartTime(updated.getStartTime());
        }
        if (updated.getEndTime() != null) {
            existing.setEndTime(updated.getEndTime());
        }
        if (updated.getProductIds() != null) {
            existing.setProductIds(updated.getProductIds());
        }
        if (updated.getEnabled() != null) {
            existing.setEnabled(updated.getEnabled());
        }
        if (updated.getPriority() != null) {
            existing.setPriority(updated.getPriority());
        }
        if (updated.getType() != null) {
            existing.setType(updated.getType());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return promotionRepository.save(existing);
    }

    public void delete(Long id) {
        promotionRepository.deleteById(id);
    }

    public PromotionStatus calculateStatus(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = promotion.getStartTime();
        LocalDateTime endTime = promotion.getEndTime();
        LocalDateTime preheatTime = promotion.getPreheatTime();

        if (now.isAfter(endTime)) {
            return PromotionStatus.ENDED;
        }
        if (preheatTime != null && !now.isBefore(preheatTime) && now.isBefore(startTime)) {
            return PromotionStatus.PREHEATING;
        }
        if (!now.isBefore(startTime) && !now.isAfter(endTime)) {
            return PromotionStatus.ACTIVE;
        }
        return PromotionStatus.NOT_STARTED;
    }

    public Promotion enable(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在, id=" + id));
        promotion.setEnabled(true);
        promotion.setUpdatedAt(LocalDateTime.now());
        return promotionRepository.save(promotion);
    }

    public Promotion disable(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在, id=" + id));
        promotion.setEnabled(false);
        promotion.setUpdatedAt(LocalDateTime.now());
        return promotionRepository.save(promotion);
    }
}
