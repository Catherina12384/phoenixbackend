package com.phoenix.controller;

import com.phoenix.dto.DealerDto;
import com.phoenix.dto.DealerRequest;
import com.phoenix.dto.DealerUpdateRequest;
import com.phoenix.dto.ProductDto;
import com.phoenix.dto.ProductRequest;
import com.phoenix.service.DealerService;
import com.phoenix.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** ADMIN only. Product UPDATE moved to PUT /api/products/{id} so staff can use it. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DealerService dealerService;
    private final ProductService productService;

    public AdminController(DealerService dealerService, ProductService productService) {
        this.dealerService = dealerService;
        this.productService = productService;
    }

    @PostMapping("/dealers")
    @ResponseStatus(HttpStatus.CREATED)
    public DealerDto createDealer(@Valid @RequestBody DealerRequest request) {
        return dealerService.create(request);
    }

    @PutMapping("/dealers/{id}")
    public DealerDto updateDealer(@PathVariable String id, @Valid @RequestBody DealerUpdateRequest request) {
        return dealerService.update(id, request);
    }

    @DeleteMapping("/dealers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDealer(@PathVariable String id) {
        dealerService.delete(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.delete(id);
    }
}
