package com.b2c.flash_sale_b2c_UTC2.auth.repository;

import com.b2c.flash_sale_b2c_UTC2.auth.entity.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {
    Optional<AuthAccount> findByProviderAndProviderAccountId(String provider, String providerAccountId);
}
