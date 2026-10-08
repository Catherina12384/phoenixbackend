package com.phoenix.controller;

import com.phoenix.dto.PageDto;
import com.phoenix.dto.ProductDto;
import com.phoenix.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<ProductDto> list(@RequestParam(name = "dealer", required = false) List<String> dealer,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "24") int size) {
        return service.search(dealer, category, q, page, size);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return service.categories();
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return service.get(id);
    }
}
