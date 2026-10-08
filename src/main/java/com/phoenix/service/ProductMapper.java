package com.phoenix.service;

import com.phoenix.dto.ProductDto;
import com.phoenix.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getImageUrl(),
                product.isInStock(),
                product.getSpecs(),
                product.getDealer().getId(),
                product.getDealer().getName());
    }
}
