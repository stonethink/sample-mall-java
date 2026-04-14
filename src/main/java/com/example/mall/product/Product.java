package com.example.mall.product;

import java.util.List;

public class Product {

    private Long id;
    private String name;
    private String sku;
    private Integer stock;
    private Integer price; // 单位：分
    private List<String> categoryIds; // 关联的分类ID列表

    public Product() {
    }

    public Product(Long id, String name, String sku, Integer stock, Integer price) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.stock = stock;
        this.price = price;
    }

    public Product(Long id, String name, String sku, Integer stock, Integer price, List<String> categoryIds) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.stock = stock;
        this.price = price;
        this.categoryIds = categoryIds;
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
}
