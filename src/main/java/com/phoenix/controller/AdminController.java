package com.phoenix.controller;

import com.phoenix.service.DealerService;
import com.phoenix.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Everything under /api/admin requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DealerService dealers;
    private final ProductService products;

    public AdminController(DealerService dealers, ProductService products) {
        this.dealers = dealers;
        this.products = products;
    }

    @PostMapping("/dealers")
    @ResponseStatus(HttpStatus.CREATED)
    public DealerDto createDealer(@Valid @RequestBody DealerRequest r) {
        return dealers.create(r);
    }

    @PutMapping("/dealers/{id}")
    public DealerDto updateDealer(@PathVariable String id, @Valid @RequestBody DealerUpdateRequest r) {
        return dealers.update(id, r);
    }

    @DeleteMapping("/dealers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDealer(@PathVariable String id) {
        dealers.delete(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@Valid @RequestBody ProductRequest r) {
        return products.create(r);
    }

    @PutMapping("/products/{id}")
    public ProductDto updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest r) {
        return products.update(id, r);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        products.delete(id);
    }
}