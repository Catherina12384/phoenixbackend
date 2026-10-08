package com.phoenix.service;

import com.phoenix.dto.DealerDto;
import com.phoenix.dto.DealerRequest;
import com.phoenix.dto.DealerUpdateRequest;
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
    private final DealerMapper mapper;

    public DealerService(DealerRepository dealers, ProductRepository products, DealerMapper mapper) {
        this.dealers = dealers;
        this.products = products;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DealerDto> list() {
        return dealers.findAll(Sort.by("sortOrder", "name"))
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public DealerDto create(DealerRequest request) {
        if (dealers.existsById(request.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Dealer id already exists: " + request.id());
        }

        Dealer dealer = new Dealer();
        dealer.setId(request.id());
        dealer.setName(request.name().trim());
        dealer.setLogoUrl(blankToNull(request.logo()));
        dealer.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());

        return mapper.toDto(dealers.save(dealer));
    }

    @Transactional
    public DealerDto update(String id, DealerUpdateRequest request) {
        Dealer dealer = dealers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dealer not found"));

        dealer.setName(request.name().trim());
        dealer.setLogoUrl(blankToNull(request.logo()));
        if (request.sortOrder() != null) {
            dealer.setSortOrder(request.sortOrder());
        }

        return mapper.toDto(dealer);
    }

    @Transactional
    public void delete(String id) {
        if (!dealers.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer not found");
        }
        if (products.existsByDealer_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dealer still has products. Move or delete them first.");
        }
        dealers.deleteById(id);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
