package com.phoenix.service;

import com.phoenix.audit.AuditAction;
import com.phoenix.audit.AuditLogger;
import com.phoenix.dto.PageDto;
import com.phoenix.dto.ProductDto;
import com.phoenix.dto.ProductRequest;
import com.phoenix.entity.Dealer;
import com.phoenix.entity.Product;
import com.phoenix.repository.DealerRepository;
import com.phoenix.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository products;
    private final DealerRepository dealers;
    private final ProductMapper mapper;
    private final ProductSpecificationFactory specificationFactory;
    private final AuditLogger audit;

    public ProductService(ProductRepository products,
                          DealerRepository dealers,
                          ProductMapper mapper,
                          ProductSpecificationFactory specificationFactory,
                          AuditLogger audit) {
        this.products = products;
        this.dealers = dealers;
        this.mapper = mapper;
        this.specificationFactory = specificationFactory;
        this.audit = audit;
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
        log.debug("Product search dealers={} category={} q={} -> {} of {}", dealerIds, category, query,
                result.getNumberOfElements(), result.getTotalElements());

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
        Product saved = products.saveAndFlush(product);
        audit.log(AuditAction.PRODUCT_CREATED, "product", String.valueOf(saved.getId()), true, snapshot(saved));
        log.info("Product {} created: {}", saved.getId(), saved.getName());
        return mapper.toDto(saved);
    }

    /** Allowed for ADMIN and STAFF (see SecurityConfig). The audit row records every changed field, old and new. */
    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        Product product = find(id);
        Map<String, Object> before = snapshot(product);
        apply(product, request);
        products.saveAndFlush(product);
        Map<String, Object> changes = diff(before, snapshot(product));

        audit.log(AuditAction.PRODUCT_UPDATED, "product", String.valueOf(id), true, Map.of("changes", changes));
        log.info("Product {} updated, changed fields: {}", id, changes.keySet());
        return mapper.toDto(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = find(id);
        Map<String, Object> snapshot = snapshot(product);
        products.delete(product);
        products.flush();
        audit.log(AuditAction.PRODUCT_DELETED, "product", String.valueOf(id), true, snapshot);
        log.info("Product {} deleted", id);
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

    private static Map<String, Object> snapshot(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", p.getName());
        m.put("description", abbreviate(p.getDescription()));
        m.put("category", p.getCategory());
        m.put("price", p.getPrice() == null ? null : p.getPrice().toPlainString());
        m.put("imageUrl", p.getImageUrl());
        m.put("inStock", p.isInStock());
        m.put("specs", p.getSpecs());
        m.put("dealerId", p.getDealer() == null ? null : p.getDealer().getId());
        return m;
    }

    private static Map<String, Object> diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        after.forEach((field, now) -> {
            Object was = before.get(field);
            if (!Objects.equals(was, now)) {
                Map<String, Object> pair = new HashMap<>();
                pair.put("from", was);
                pair.put("to", now);
                changes.put(field, pair);
            }
        });
        return changes;
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
