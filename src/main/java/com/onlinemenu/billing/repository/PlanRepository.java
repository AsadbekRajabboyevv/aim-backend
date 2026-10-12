package com.onlinemenu.billing.repository;

import com.onlinemenu.billing.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

  List<Plan> findAllByOrderBySortOrderAscMonthlyPriceAsc();

  List<Plan> findAllByActiveTrueOrderBySortOrderAscMonthlyPriceAsc();

  Optional<Plan> findBySlug(String slug);

  boolean existsBySlug(String slug);
}
