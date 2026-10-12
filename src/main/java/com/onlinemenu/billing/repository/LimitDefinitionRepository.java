package com.onlinemenu.billing.repository;

import com.onlinemenu.billing.domain.LimitDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitDefinitionRepository extends JpaRepository<LimitDefinition, UUID> {

  List<LimitDefinition> findAllByOrderBySortOrderAscKeyAsc();

  Optional<LimitDefinition> findByKey(String key);

  boolean existsByKey(String key);
}
