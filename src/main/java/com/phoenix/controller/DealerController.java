package com.phoenix.controller;

import com.phoenix.dto.DealerDto;
import com.phoenix.service.DealerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dealers")
public class DealerController {
    private final DealerService service;

    public DealerController(DealerService service) {
        this.service = service;
    }

    @GetMapping
    public List<DealerDto> list() {
        return service.list();
    }
}
