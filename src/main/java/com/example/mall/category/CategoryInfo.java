package com.example.mall.category;

public class CategoryInfo {
    private String id;
    private String name;
    private String fullPath;

    public CategoryInfo() {
    }

    public CategoryInfo(String id, String name, String fullPath) {
        this.id = id;
        this.name = name;
        this.fullPath = fullPath;
    }

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

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }
}
