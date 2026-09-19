package com.b2c.flash_sale_b2c_UTC2.user.repository;

import com.b2c.flash_sale_b2c_UTC2.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByCode(String code);
}
