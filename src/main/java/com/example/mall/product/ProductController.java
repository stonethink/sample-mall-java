package com.example.mall.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping({"/api/products", "/product"})
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> listAll() {
        return productService.listAll();
    }

    // 对齐 mall-admin：/product/list 查询商品列表
    @GetMapping("/list")
    public List<Product> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) String categoryId) {
        if (categoryId != null && !categoryId.isEmpty()) {
            return productService.listByCategory(categoryId);
        }
        return productService.searchByName(keyword);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        Product created = productService.create(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        Product updated = productService.update(id, product);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 对齐 mall-admin：/product/simpleList 模糊查询
    @GetMapping("/simpleList")
    public List<Product> simpleList(@RequestParam(value = "keyword", required = false) String keyword) {
        return productService.searchByName(keyword);
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam(name = "q", required = false) String keyword) {
        return productService.searchByName(keyword);
    }
}
