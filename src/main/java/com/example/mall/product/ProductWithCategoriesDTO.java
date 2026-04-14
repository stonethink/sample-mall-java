package com.example.mall.product;

import com.example.mall.category.CategoryInfo;

import java.util.List;

public class ProductWithCategoriesDTO {
    private Long id;
    private String name;
    private String sku;
    private Integer stock;
    private Integer price;
    private List<String> categoryIds;
    private List<CategoryInfo> categories;

    public ProductWithCategoriesDTO() {
    }

    public ProductWithCategoriesDTO(Long id, String name, String sku, Integer stock, Integer price, 
                                     List<String> categoryIds, List<CategoryInfo> categories) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.stock = stock;
        this.price = price;
        this.categoryIds = categoryIds;
        this.categories = categories;
    }

    // 从 Product 构造 DTO
    public static ProductWithCategoriesDTO fromProduct(Product product, List<CategoryInfo> categories) {
        return new ProductWithCategoriesDTO(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getStock(),
                product.getPrice(),
                product.getCategoryIds(),
                categories
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<CategoryInfo> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryInfo> categories) {
        this.categories = categories;
    }
}
