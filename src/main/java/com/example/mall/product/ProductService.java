package com.example.mall.product;

import com.example.mall.category.CategoryInfo;
import com.example.mall.category.CategoryRepository;
import com.example.mall.category.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
    }

    public List<Product> listAll() {
        return productRepository.findAll();
    }

    public List<ProductWithCategoriesDTO> listAllWithCategories() {
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<Product> listByCategory(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            return productRepository.findAll();
        }
        return productRepository.findAll().stream()
                .filter(p -> p.getCategoryIds() != null && p.getCategoryIds().contains(categoryId))
                .collect(Collectors.toList());
    }

    public List<ProductWithCategoriesDTO> listByCategoryWithCategories(String categoryId) {
        return listByCategory(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<ProductWithCategoriesDTO> findByIdWithCategories(Long id) {
        return productRepository.findById(id)
                .map(this::convertToDTO);
    }

    private ProductWithCategoriesDTO convertToDTO(Product product) {
        List<CategoryInfo> categories = categoryService.convertToCategoryInfoList(product.getCategoryIds());
        return ProductWithCategoriesDTO.fromProduct(product, categories);
    }

    public Product create(Product product) {
        // 验证分类ID是否有效
        validateCategoryIds(product.getCategoryIds());
        return productRepository.save(product);
    }

    private void validateCategoryIds(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        for (String categoryId : categoryIds) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new IllegalArgumentException("分类不存在: " + categoryId);
            }
        }
    }

    public Product update(Long id, Product updated) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found, id=" + id));
        // 验证分类ID是否有效
        validateCategoryIds(updated.getCategoryIds());
        existing.setName(updated.getName());
        existing.setSku(updated.getSku());
        existing.setStock(updated.getStock());
        existing.setPrice(updated.getPrice());
        existing.setCategoryIds(updated.getCategoryIds());
        return productRepository.save(existing);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public List<Product> searchByName(String keyword) {
        return productRepository.findByNameContains(keyword);
    }
}
