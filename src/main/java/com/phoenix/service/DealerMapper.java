package com.phoenix.service;

import com.phoenix.dto.DealerDto;
import com.phoenix.entity.Dealer;
import org.springframework.stereotype.Component;

@Component
public class DealerMapper {
    public DealerDto toDto(Dealer dealer) {
        return new DealerDto(dealer.getId(), dealer.getName(), dealer.getLogoUrl());
    }
}
