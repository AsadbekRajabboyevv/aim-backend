package com.onlinemenu.menu.repository;

import com.onlinemenu.menu.domain.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

  List<MenuCategory> findAllByCafeIdOrderBySortOrderAscCreatedAtAsc(UUID cafeId);

  List<MenuCategory> findAllByCafeIdAndActiveTrueOrderBySortOrderAscCreatedAtAsc(UUID cafeId);

  Optional<MenuCategory> findBySlugAndCafeId(String slug, UUID cafeId);

  @Query("select coalesce(max(c.sortOrder), -1) from MenuCategory c where c.cafe.id = :cafeId")
  int findMaxSortOrder(UUID cafeId);

  boolean existsBySlug(String slug);
}
