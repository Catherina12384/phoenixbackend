package com.phoenix.service;

import com.phoenix.audit.AuditAction;
import com.phoenix.audit.AuditLogger;
import com.phoenix.dto.DealerDto;
import com.phoenix.dto.DealerRequest;
import com.phoenix.dto.DealerUpdateRequest;
import com.phoenix.entity.Dealer;
import com.phoenix.repository.DealerRepository;
import com.phoenix.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DealerService {
    private static final Logger log = LoggerFactory.getLogger(DealerService.class);

    private final DealerRepository dealers;
    private final ProductRepository products;
    private final DealerMapper mapper;
    private final AuditLogger audit;

    public DealerService(DealerRepository dealers, ProductRepository products, DealerMapper mapper, AuditLogger audit) {
        this.dealers = dealers;
        this.products = products;
        this.mapper = mapper;
        this.audit = audit;
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

        Dealer saved = dealers.saveAndFlush(dealer);
        audit.log(AuditAction.DEALER_CREATED, "dealer", saved.getId(), true, Map.of("name", saved.getName()));
        log.info("Dealer {} created", saved.getId());
        return mapper.toDto(saved);
    }

    @Transactional
    public DealerDto update(String id, DealerUpdateRequest request) {
        Dealer dealer = dealers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dealer not found"));

        Map<String, Object> changes = new HashMap<>();
        String newName = request.name().trim();
        String newLogo = blankToNull(request.logo());
        if (!newName.equals(dealer.getName())) changes.put("name", pair(dealer.getName(), newName));
        if (!java.util.Objects.equals(newLogo, dealer.getLogoUrl())) changes.put("logo", pair(dealer.getLogoUrl(), newLogo));

        dealer.setName(newName);
        dealer.setLogoUrl(newLogo);
        if (request.sortOrder() != null) {
            if (request.sortOrder() != dealer.getSortOrder()) changes.put("sortOrder", pair(dealer.getSortOrder(), request.sortOrder()));
            dealer.setSortOrder(request.sortOrder());
        }
        dealers.saveAndFlush(dealer);

        audit.log(AuditAction.DEALER_UPDATED, "dealer", id, true, Map.of("changes", changes));
        log.info("Dealer {} updated, changed fields: {}", id, changes.keySet());
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
        dealers.flush();
        audit.log(AuditAction.DEALER_DELETED, "dealer", id, true, null);
        log.info("Dealer {} deleted", id);
    }

    private static Map<String, Object> pair(Object from, Object to) {
        Map<String, Object> m = new HashMap<>();
        m.put("from", from);
        m.put("to", to);
        return m;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
