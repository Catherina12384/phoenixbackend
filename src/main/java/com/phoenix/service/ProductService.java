package com.phoenix.service;

import com.phoenix.entity.Dealer;
import com.phoenix.repository.DealerRepository;
import com.phoenix.entity.Product;
import com.phoenix.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository products;
    private final DealerRepository dealers;

    public ProductService(ProductRepository products, DealerRepository dealers) {
        this.products = products;
        this.dealers = dealers;
    }

    /** Empty/null dealer list = no dealer filter (same rule the front end documents). */
    @Transactional(readOnly = true)
    public PageDto<ProductDto> search(List<String> dealerIds, String category, String q, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Product> p = products.findAll(filter(dealerIds, category, q), pageable);
        return new PageDto<>(p.getContent().stream().map(ProductService::toDto).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return products.findCategories();
    }

    @Transactional
    public ProductDto create(ProductRequest r) {
        Product p = new Product();
        apply(p, r);
        return toDto(products.save(p));
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest r) {
        Product p = find(id);
        apply(p, r);
        return toDto(p);
    }

    @Transactional
    public void delete(Long id) {
        products.delete(find(id));
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void apply(Product p, ProductRequest r) {
        Dealer dealer = dealers.findById(r.dealerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown dealer: " + r.dealerId()));
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setCategory(r.category().trim());
        p.setPrice(r.price());
        p.setImageUrl(r.imageUrl() == null || r.imageUrl().isBlank() ? null : r.imageUrl().trim());
        p.setInStock(r.inStock() == null || r.inStock());
        p.setSpecs(r.specs());
        p.setDealer(dealer);
    }

    private static Specification<Product> filter(List<String> dealerIds, String category, String q) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (dealerIds != null && !dealerIds.isEmpty()) {
                preds.add(root.get("dealer").get("id").in(dealerIds));
            }
            if (category != null && !category.isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    static ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getPrice(),
                p.getImageUrl(), p.isInStock(), p.getSpecs(), p.getDealer().getId(), p.getDealer().getName());
    }
}