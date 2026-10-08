package com.phoenix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dealers")
@Getter @Setter @NoArgsConstructor
public class Dealer {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}