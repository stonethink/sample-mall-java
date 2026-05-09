package com.example.mall.promotion;

import com.example.mall.user.User;
import com.example.mall.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ResponseEntity<?> listAll(HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        List<Promotion> promotions = promotionService.findAll();
        promotions.forEach(p -> p.setStatus(promotionService.calculateStatus(p)));
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        return promotionService.findById(id)
                .map(p -> {
                    p.setStatus(promotionService.calculateStatus(p));
                    return ResponseEntity.ok(p);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Promotion promotion, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            Promotion created = promotionService.create(promotion);
            created.setStatus(promotionService.calculateStatus(created));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("INVALID_REQUEST", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Promotion promotion, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            Promotion updated = promotionService.update(id, promotion);
            updated.setStatus(promotionService.calculateStatus(updated));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("PROMOTION_NOT_FOUND", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<?> enable(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            Promotion promotion = promotionService.enable(id);
            promotion.setStatus(promotionService.calculateStatus(promotion));
            return ResponseEntity.ok(promotion);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("PROMOTION_NOT_FOUND", e.getMessage());
        }
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disable(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            Promotion promotion = promotionService.disable(id);
            promotion.setStatus(promotionService.calculateStatus(promotion));
            return ResponseEntity.ok(promotion);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("PROMOTION_NOT_FOUND", e.getMessage());
        }
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(String error, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
