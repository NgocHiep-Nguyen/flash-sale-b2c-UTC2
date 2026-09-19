package com.b2c.flash_sale_b2c_UTC2.user.repository;

import com.b2c.flash_sale_b2c_UTC2.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
}
