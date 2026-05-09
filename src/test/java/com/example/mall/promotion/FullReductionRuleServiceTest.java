package com.example.mall.promotion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FullReductionRuleServiceTest {

    private FullReductionRuleService ruleService;
    private FullReductionRuleRepository ruleRepository;

    @BeforeEach
    void setUp() {
        ruleRepository = new FullReductionRuleRepository();
        ruleService = new FullReductionRuleService(ruleRepository);
    }

    @Test
    void addRule_shouldSucceedWithValidData() {
        FullReductionRule rule = new FullReductionRule();
        rule.setPromotionId(1L);
        rule.setType(RuleType.LADDER);
        rule.setFullAmount(10000);
        rule.setReductionAmount(1000);
        rule.setLevel(1);

        FullReductionRule created = ruleService.addRule(1L, rule);

        assertNotNull(created.getId());
        assertEquals(1L, created.getPromotionId());
    }

    @Test
    void addRule_shouldThrowWhenFullAmountNotPositive() {
        FullReductionRule rule = new FullReductionRule();
        rule.setPromotionId(1L);
        rule.setFullAmount(0);
        rule.setReductionAmount(1000);
        rule.setLevel(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ruleService.addRule(1L, rule));
        assertEquals("满足金额必须大于0", exception.getMessage());
    }

    @Test
    void addRule_shouldThrowWhenReductionNotLessThanFull() {
        FullReductionRule rule = new FullReductionRule();
        rule.setPromotionId(1L);
        rule.setFullAmount(1000);
        rule.setReductionAmount(1000);
        rule.setLevel(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ruleService.addRule(1L, rule));
        assertEquals("减免金额必须小于满足金额", exception.getMessage());
    }

    @Test
    void batchSetRules_shouldDeleteOldAndInsertNew() {
        FullReductionRule oldRule = new FullReductionRule();
        oldRule.setPromotionId(1L);
        oldRule.setFullAmount(5000);
        oldRule.setReductionAmount(500);
        oldRule.setLevel(1);
        ruleService.addRule(1L, oldRule);

        FullReductionRule newRule = new FullReductionRule();
        newRule.setPromotionId(1L);
        newRule.setFullAmount(10000);
        newRule.setReductionAmount(1000);
        newRule.setLevel(1);

        ruleService.batchSetRules(1L, Arrays.asList(newRule));

        List<FullReductionRule> rules = ruleService.findByPromotionId(1L);
        assertEquals(1, rules.size());
        assertEquals(10000, rules.get(0).getFullAmount());
    }

    @Test
    void batchSetRules_shouldThrowWhenLevelNotIncreasing() {
        FullReductionRule rule1 = new FullReductionRule();
        rule1.setPromotionId(1L);
        rule1.setFullAmount(20000);
        rule1.setReductionAmount(2000);
        rule1.setLevel(1);

        FullReductionRule rule2 = new FullReductionRule();
        rule2.setPromotionId(1L);
        rule2.setFullAmount(10000);
        rule2.setReductionAmount(1000);
        rule2.setLevel(2);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ruleService.batchSetRules(1L, Arrays.asList(rule1, rule2)));
        assertTrue(exception.getMessage().contains("level 越大，fullAmount 必须越大"));
    }
}
