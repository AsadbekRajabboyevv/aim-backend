package com.onlinemenu.billing.repository;

import com.onlinemenu.billing.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByOrderNumber(Long orderNumber);

  Optional<Payment> findByOrderNumberAndUserId(Long orderNumber, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Payment p where p.orderNumber = :orderNumber")
  Optional<Payment> findByOrderNumberForUpdate(@Param("orderNumber") Long orderNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Payment p where p.provider = :provider and p.providerTransactionId = :transactionId")
  Optional<Payment> findByProviderTransactionForUpdate(@Param("provider") String provider,
      @Param("transactionId") String transactionId);

  @Query("""
      select p from Payment p
      where p.provider = :provider and p.providerTime between :from and :to
      order by p.providerTime
      """)
  List<Payment> findAllByProviderTimeBetween(@Param("provider") String provider,
      @Param("from") long from, @Param("to") long to);

  List<Payment> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  @Query(value = """
      select p from Payment p
      where (:status is null or p.status = :status)
        and (:provider is null or p.provider = :provider)
      """)
  Page<Payment> search(@Param("status") com.onlinemenu.billing.domain.PaymentStatus status,
      @Param("provider") String provider, Pageable pageable);

  boolean existsByPlanId(UUID planId);
}
