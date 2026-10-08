package com.phoenix.service;

import com.phoenix.entity.Dealer;
import com.phoenix.repository.DealerRepository;
import com.phoenix.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DealerService {
    private final DealerRepository dealers;
    private final ProductRepository products;

    public DealerService(DealerRepository dealers, ProductRepository products) {
        this.dealers = dealers;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<DealerDto> list() {
        return dealers.findAll(Sort.by("sortOrder", "name")).stream().map(DealerService::toDto).toList();
    }

    @Transactional
    public DealerDto create(DealerRequest r) {
        if (dealers.existsById(r.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dealer id already exists: " + r.id());
        }
        Dealer d = new Dealer();
        d.setId(r.id());
        d.setName(r.name().trim());
        d.setLogoUrl(blankToNull(r.logo()));
        d.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
        return toDto(dealers.save(d));
    }

    @Transactional
    public DealerDto update(String id, DealerUpdateRequest r) {
        Dealer d = dealers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer not found"));
        d.setName(r.name().trim());
        d.setLogoUrl(blankToNull(r.logo()));
        if (r.sortOrder() != null) d.setSortOrder(r.sortOrder());
        return toDto(d);
    }

    @Transactional
    public void delete(String id) {
        if (!dealers.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer not found");
        if (products.existsByDealer_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dealer still has products. Move or delete them first.");
        }
        dealers.deleteById(id);
    }

    static DealerDto toDto(Dealer d) {
        return new DealerDto(d.getId(), d.getName(), d.getLogoUrl());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}