package com.example.mall.category;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class CategoryRepository {

    private final ConcurrentHashMap<String, Category> store = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadCategoriesFromJson();
    }

    private void loadCategoriesFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("categories.json");
            if (!resource.exists()) {
                // 如果文件不存在，初始化默认数据
                initializeDefaultCategories();
                return;
            }
            InputStream inputStream = resource.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            List<Category> categories = objectMapper.readValue(inputStream, new TypeReference<List<Category>>(){});

            for (Category category : categories) {
                store.put(category.getId(), category);
            }
        } catch (IOException e) {
            System.err.println("Failed to load categories from JSON: " + e.getMessage());
            initializeDefaultCategories();
        }
    }

    private void initializeDefaultCategories() {
        // 初始化默认分类数据
        save(new Category("cat-001", "食品生鲜", null));
        save(new Category("cat-002", "水果", "cat-001"));
        save(new Category("cat-003", "零食", "cat-001"));
        save(new Category("cat-004", "图书音像", null));
        save(new Category("cat-005", "文学小说", "cat-004"));
        save(new Category("cat-006", "计算机书籍", "cat-004"));
        save(new Category("cat-007", "日用百货", null));
        save(new Category("cat-008", "洗护用品", "cat-007"));
        save(new Category("cat-009", "纸品", "cat-007"));
        save(new Category("cat-010", "办公文具", null));
    }

    public List<Category> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Category> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Category save(Category category) {
        if (category.getId() == null || category.getId().isEmpty()) {
            category.setId(UUID.randomUUID().toString());
        }
        store.put(category.getId(), category);
        return category;
    }

    public void deleteById(String id) {
        store.remove(id);
    }

    public List<Category> findByParentId(String parentId) {
        return store.values().stream()
                .filter(c -> parentId == null ? c.isRoot() : parentId.equals(c.getParentId()))
                .collect(Collectors.toList());
    }

    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    public boolean existsByNameAndParentId(String name, String parentId) {
        return store.values().stream()
                .anyMatch(c -> c.getName().equals(name) && 
                    (parentId == null ? c.isRoot() : parentId.equals(c.getParentId())));
    }

    public boolean hasChildren(String parentId) {
        return store.values().stream()
                .anyMatch(c -> parentId.equals(c.getParentId()));
    }

    public int getDepth(String categoryId) {
        int depth = 0;
        Category current = store.get(categoryId);
        while (current != null && !current.isRoot()) {
            depth++;
            current = store.get(current.getParentId());
        }
        return depth;
    }

    public boolean isDescendant(String ancestorId, String descendantId) {
        Category current = store.get(descendantId);
        while (current != null && !current.isRoot()) {
            if (ancestorId.equals(current.getParentId())) {
                return true;
            }
            current = store.get(current.getParentId());
        }
        return false;
    }
}
