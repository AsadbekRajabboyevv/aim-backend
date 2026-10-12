package com.onlinemenu.billing.repository;

import com.onlinemenu.billing.domain.Subscription;
import com.onlinemenu.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  Optional<Subscription> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Subscription s where s.userId = :userId")
  Optional<Subscription> findByUserIdForUpdate(@Param("userId") UUID userId);

  List<Subscription> findAllByUserIdIn(Collection<UUID> userIds);

  @Query("""
      select u from User u
      where u.role = com.onlinemenu.user.domain.Role.CAFE_ADMIN
        and (lower(u.fullName) like lower(concat('%', :search, '%'))
              or lower(u.email) like lower(concat('%', :search, '%')))
      """)
  Page<User> searchCafeAdmins(@Param("search") String search, Pageable pageable);

  boolean existsByPlanId(UUID planId);
}
