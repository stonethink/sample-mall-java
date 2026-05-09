package com.example.mall.promotion;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FullReductionRuleService {

    private final FullReductionRuleRepository ruleRepository;

    public FullReductionRuleService(FullReductionRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<FullReductionRule> findByPromotionId(Long promotionId) {
        return ruleRepository.findByPromotionId(promotionId);
    }

    public FullReductionRule addRule(Long promotionId, FullReductionRule rule) {
        rule.setPromotionId(promotionId);
        validateRule(rule);
        validateLevelUnique(promotionId, rule.getLevel(), null);
        return ruleRepository.save(rule);
    }

    public FullReductionRule updateRule(Long ruleId, FullReductionRule rule) {
        FullReductionRule existing = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在, id=" + ruleId));
        validateRule(rule);
        validateLevelUnique(existing.getPromotionId(), rule.getLevel(), ruleId);
        existing.setType(rule.getType());
        existing.setFullAmount(rule.getFullAmount());
        existing.setReductionAmount(rule.getReductionAmount());
        existing.setLevel(rule.getLevel());
        return ruleRepository.save(existing);
    }

    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    public void batchSetRules(Long promotionId, List<FullReductionRule> rules) {
        ruleRepository.deleteByPromotionId(promotionId);

        Set<Integer> levels = new HashSet<>();
        FullReductionRule prev = null;

        List<FullReductionRule> sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(FullReductionRule::getLevel))
                .collect(Collectors.toList());

        for (FullReductionRule rule : sortedRules) {
            rule.setPromotionId(promotionId);
            validateRule(rule);
            if (!levels.add(rule.getLevel())) {
                throw new IllegalArgumentException("阶梯档位 level=" + rule.getLevel() + " 重复");
            }
            if (prev != null && rule.getFullAmount() <= prev.getFullAmount()) {
                throw new IllegalArgumentException("level 越大，fullAmount 必须越大");
            }
            prev = rule;
        }

        for (FullReductionRule rule : sortedRules) {
            ruleRepository.save(rule);
        }
    }

    private void validateRule(FullReductionRule rule) {
        if (rule.getFullAmount() == null || rule.getFullAmount() <= 0) {
            throw new IllegalArgumentException("满足金额必须大于0");
        }
        if (rule.getReductionAmount() == null || rule.getReductionAmount() <= 0) {
            throw new IllegalArgumentException("减免金额必须大于0");
        }
        if (rule.getReductionAmount() >= rule.getFullAmount()) {
            throw new IllegalArgumentException("减免金额必须小于满足金额");
        }
        if (rule.getLevel() == null || rule.getLevel() < 1) {
            throw new IllegalArgumentException("阶梯等级必须大于等于1");
        }
    }

    private void validateLevelUnique(Long promotionId, Integer level, Long excludeRuleId) {
        List<FullReductionRule> existingRules = ruleRepository.findByPromotionId(promotionId);
        for (FullReductionRule r : existingRules) {
            if (r.getLevel().equals(level) && !r.getId().equals(excludeRuleId)) {
                throw new IllegalArgumentException("阶梯档位 level=" + level + " 已存在");
            }
        }
    }
}
