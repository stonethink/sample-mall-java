package com.example.mall.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "商品分类管理", description = "商品分类的增删改查接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(summary = "获取所有分类", description = "返回扁平化的分类列表")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "返回层级结构的分类树")
    public ResponseEntity<List<CategoryService.CategoryTreeNode>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取分类", description = "返回指定分类的详细信息")
    public ResponseEntity<?> getCategoryById(
            @Parameter(description = "分类ID") @PathVariable String id) {
        try {
            Category category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "获取子分类", description = "返回指定分类的直接子分类")
    public ResponseEntity<List<Category>> getSubCategories(
            @Parameter(description = "父分类ID，为空则返回根分类") @PathVariable String id) {
        return ResponseEntity.ok(categoryService.getSubCategories(id));
    }

    @PostMapping
    @Operation(summary = "创建分类", description = "创建新的商品分类")
    public ResponseEntity<?> createCategory(
            @Parameter(description = "分类名称") @RequestParam String name,
            @Parameter(description = "父分类ID，为空则创建根分类") @RequestParam(required = false) String parentId) {
        try {
            Category category = categoryService.createCategory(name, parentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类", description = "更新指定分类的信息")
    public ResponseEntity<?> updateCategory(
            @Parameter(description = "分类ID") @PathVariable String id,
            @Parameter(description = "分类名称") @RequestParam String name,
            @Parameter(description = "父分类ID") @RequestParam(required = false) String parentId) {
        try {
            Category category = categoryService.updateCategory(id, name, parentId);
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除指定分类（要求无子分类且无商品关联）")
    public ResponseEntity<?> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable String id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(createSuccessResponse("分类删除成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return response;
    }
}
