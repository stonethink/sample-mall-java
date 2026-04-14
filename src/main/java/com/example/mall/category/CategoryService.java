package com.example.mall.category;

import com.example.mall.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final int MAX_DEPTH = 5;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    public Category createCategory(String name, String parentId) {
        // 检查同级分类名称是否重复
        if (categoryRepository.existsByNameAndParentId(name, parentId)) {
            throw new IllegalArgumentException("分类名称已存在");
        }

        // 检查父分类是否存在
        if (parentId != null && !parentId.isEmpty() && !categoryRepository.existsById(parentId)) {
            throw new IllegalArgumentException("父分类不存在");
        }

        // 检查层级深度
        if (parentId != null && !parentId.isEmpty()) {
            int parentDepth = categoryRepository.getDepth(parentId);
            if (parentDepth + 1 >= MAX_DEPTH) {
                throw new IllegalArgumentException("分类层级不能超过5级");
            }
        }

        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        return categoryRepository.save(category);
    }

    public Category updateCategory(String id, String name, String parentId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));

        // 检查同级分类名称是否重复（排除自己）
        if (!category.getName().equals(name) && 
            categoryRepository.existsByNameAndParentId(name, parentId)) {
            throw new IllegalArgumentException("分类名称已存在");
        }

        // 检查父分类是否存在
        if (parentId != null && !parentId.isEmpty() && !categoryRepository.existsById(parentId)) {
            throw new IllegalArgumentException("父分类不存在");
        }

        // 检查循环引用
        if (parentId != null && !parentId.isEmpty() && 
            (id.equals(parentId) || categoryRepository.isDescendant(id, parentId))) {
            throw new IllegalArgumentException("不能将分类设置为其子分类的后代");
        }

        // 检查层级深度
        if (parentId != null && !parentId.isEmpty()) {
            int parentDepth = categoryRepository.getDepth(parentId);
            int currentDepth = categoryRepository.getDepth(id);
            if (parentDepth + 1 + currentDepth >= MAX_DEPTH) {
                throw new IllegalArgumentException("分类层级不能超过5级");
            }
        }

        category.setName(name);
        category.setParentId(parentId);
        return categoryRepository.save(category);
    }

    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));

        // 检查是否有子分类
        if (categoryRepository.hasChildren(id)) {
            throw new IllegalArgumentException("请先删除子分类");
        }

        // 检查是否有商品关联
        if (hasProductsInCategory(id)) {
            throw new IllegalArgumentException("该分类下存在商品，无法删除");
        }

        categoryRepository.deleteById(id);
    }

    private boolean hasProductsInCategory(String categoryId) {
        return productRepository.findAll().stream()
                .anyMatch(p -> p.getCategoryIds() != null && 
                    p.getCategoryIds().contains(categoryId));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
    }

    public List<Category> getSubCategories(String parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public List<CategoryTreeNode> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<String, CategoryTreeNode> nodeMap = new HashMap<>();
        
        // 创建所有节点
        for (Category category : allCategories) {
            CategoryTreeNode node = new CategoryTreeNode();
            node.setId(category.getId());
            node.setName(category.getName());
            node.setParentId(category.getParentId());
            node.setChildren(new ArrayList<>());
            nodeMap.put(category.getId(), node);
        }
        
        // 构建树结构
        List<CategoryTreeNode> rootNodes = new ArrayList<>();
        for (CategoryTreeNode node : nodeMap.values()) {
            if (node.getParentId() == null || node.getParentId().isEmpty()) {
                rootNodes.add(node);
            } else {
                CategoryTreeNode parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        
        return rootNodes;
    }

    public boolean existsById(String id) {
        return categoryRepository.existsById(id);
    }

    /**
     * 构建分类的完整路径（如：食品生鲜 > 水果 > 进口水果）
     */
    public String buildCategoryPath(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            return "";
        }
        
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return "";
        }
        
        List<String> pathNames = new ArrayList<>();
        Category current = category;
        
        while (current != null) {
            pathNames.add(0, current.getName());
            if (current.isRoot()) {
                break;
            }
            current = categoryRepository.findById(current.getParentId()).orElse(null);
        }
        
        return String.join(" > ", pathNames);
    }

    /**
     * 获取单个分类的 CategoryInfo（包含完整路径）
     */
    public CategoryInfo getCategoryInfo(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            return null;
        }
        
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return null;
        }
        
        String fullPath = buildCategoryPath(categoryId);
        return new CategoryInfo(category.getId(), category.getName(), fullPath);
    }

    /**
     * 将分类ID列表转换为 CategoryInfo 列表
     */
    public List<CategoryInfo> convertToCategoryInfoList(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return categoryIds.stream()
                .map(this::getCategoryInfo)
                .filter(info -> info != null)
                .collect(Collectors.toList());
    }

    // 树节点DTO
    public static class CategoryTreeNode {
        private String id;
        private String name;
        private String parentId;
        private List<CategoryTreeNode> children;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public List<CategoryTreeNode> getChildren() {
            return children;
        }

        public void setChildren(List<CategoryTreeNode> children) {
            this.children = children;
        }
    }
}
