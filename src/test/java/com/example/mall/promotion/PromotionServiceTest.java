package com.example.mall.promotion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PromotionServiceTest {

    private PromotionService promotionService;
    private PromotionRepository promotionRepository;

    @BeforeEach
    void setUp() {
        promotionRepository = new PromotionRepository();
        promotionService = new PromotionService(promotionRepository);
    }

    @Test
    void create_shouldSetDefaultValues() {
        Promotion promotion = new Promotion();
        promotion.setName("春季大促");
        promotion.setStartTime(LocalDateTime.now().plusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        Promotion created = promotionService.create(promotion);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertEquals(Boolean.TRUE, created.getEnabled());
        assertEquals(PromotionType.FULL_REDUCTION, created.getType());
    }

    @Test
    void create_shouldThrowWhenNameTooShort() {
        Promotion promotion = new Promotion();
        promotion.setName("A");
        promotion.setStartTime(LocalDateTime.now().plusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> promotionService.create(promotion));
        assertEquals("活动名称长度必须在2-50个字符之间", exception.getMessage());
    }

    @Test
    void create_shouldThrowWhenEndTimeBeforeStartTime() {
        Promotion promotion = new Promotion();
        promotion.setName("春季大促");
        promotion.setStartTime(LocalDateTime.now().plusDays(7));
        promotion.setEndTime(LocalDateTime.now().plusDays(1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> promotionService.create(promotion));
        assertEquals("结束时间必须晚于开始时间", exception.getMessage());
    }

    @Test
    void calculateStatus_shouldReturnNotStarted() {
        Promotion promotion = new Promotion();
        promotion.setPreheatTime(LocalDateTime.now().plusDays(1));
        promotion.setStartTime(LocalDateTime.now().plusDays(2));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        PromotionStatus status = promotionService.calculateStatus(promotion);

        assertEquals(PromotionStatus.NOT_STARTED, status);
    }

    @Test
    void calculateStatus_shouldReturnActive() {
        Promotion promotion = new Promotion();
        promotion.setStartTime(LocalDateTime.now().minusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        PromotionStatus status = promotionService.calculateStatus(promotion);

        assertEquals(PromotionStatus.ACTIVE, status);
    }

    @Test
    void calculateStatus_shouldReturnEnded() {
        Promotion promotion = new Promotion();
        promotion.setStartTime(LocalDateTime.now().minusDays(7));
        promotion.setEndTime(LocalDateTime.now().minusDays(1));

        PromotionStatus status = promotionService.calculateStatus(promotion);

        assertEquals(PromotionStatus.ENDED, status);
    }

    @Test
    void calculateStatus_shouldReturnPreheating() {
        Promotion promotion = new Promotion();
        promotion.setPreheatTime(LocalDateTime.now().minusDays(1));
        promotion.setStartTime(LocalDateTime.now().plusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        PromotionStatus status = promotionService.calculateStatus(promotion);

        assertEquals(PromotionStatus.PREHEATING, status);
    }
}
