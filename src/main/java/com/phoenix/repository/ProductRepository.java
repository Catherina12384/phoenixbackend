package com.phoenix.repository;

import com.phoenix.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsByDealer_Id(String dealerId);

    @Query("select distinct p.category from Product p order by p.category")
    List<String> findCategories();
}