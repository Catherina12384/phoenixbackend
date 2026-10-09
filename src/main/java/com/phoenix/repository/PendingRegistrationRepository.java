package com.phoenix.repository;

import com.phoenix.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, UUID> {
    @Modifying
    @Query("delete from PendingRegistration p where p.phone = :phone or lower(p.email) = :email")
    void deleteByPhoneOrEmail(@Param("phone") String phone, @Param("email") String email);
}
