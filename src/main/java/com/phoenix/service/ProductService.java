package com.phoenix.service;

import com.phoenix.dto.PageDto;
import com.phoenix.dto.ProductDto;
import com.phoenix.dto.ProductRequest;
import com.phoenix.entity.Dealer;
import com.phoenix.entity.Product;
import com.phoenix.repository.DealerRepository;
import com.phoenix.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository products;
    private final DealerRepository dealers;
    private final ProductMapper mapper;
    private final ProductSpecificationFactory specificationFactory;

    public ProductService(ProductRepository products,
                          DealerRepository dealers,
                          ProductMapper mapper,
                          ProductSpecificationFactory specificationFactory) {
        this.products = products;
        this.dealers = dealers;
        this.mapper = mapper;
        this.specificationFactory = specificationFactory;
    }

    @Transactional(readOnly = true)
    public PageDto<ProductDto> search(List<String> dealerIds, String category, String query, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        Specification<Product> specification =
                specificationFactory.create(dealerIds, category, query);

        Page<Product> result = products.findAll(specification, pageable);

        return new PageDto<>(
                result.getContent().stream().map(mapper::toDto).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return products.findCategories();
    }

    @Transactional
    public ProductDto create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return mapper.toDto(products.save(product));
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        Product product = find(id);
        apply(product, request);
        return mapper.toDto(product);
    }

    @Transactional
    public void delete(Long id) {
        products.delete(find(id));
    }

    private Product find(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void apply(Product product, ProductRequest request) {
        Dealer dealer = dealers.findById(request.dealerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown dealer: " + request.dealerId()));

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setImageUrl(blankToNull(request.imageUrl()));
        product.setInStock(request.inStock() == null || request.inStock());
        product.setSpecs(request.specs());
        product.setDealer(dealer);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
