package com.b2c.flash_sale_b2c_UTC2.payment.repository;

import com.b2c.flash_sale_b2c_UTC2.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionCode(String transactionCode);
    List<Payment> findByOrderId(Long orderId);
}
