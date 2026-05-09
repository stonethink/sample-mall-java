package com.example.mall.promotion;

public class FullReductionRule {

    private Long id;
    private Long promotionId;
    private RuleType type;
    private Integer fullAmount;
    private Integer reductionAmount;
    private Integer level;

    public FullReductionRule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Long promotionId) {
        this.promotionId = promotionId;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public Integer getFullAmount() {
        return fullAmount;
    }

    public void setFullAmount(Integer fullAmount) {
        this.fullAmount = fullAmount;
    }

    public Integer getReductionAmount() {
        return reductionAmount;
    }

    public void setReductionAmount(Integer reductionAmount) {
        this.reductionAmount = reductionAmount;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
