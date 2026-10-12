package com.onlinemenu.menu.repository;

import com.onlinemenu.menu.domain.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  long countByCafeOwnerId(UUID ownerId);

  @EntityGraph(attributePaths = "category")
  @Query("""
      select p from Product p
      where p.cafe.id = :cafeId
      order by p.category.sortOrder asc, p.sortOrder asc, p.createdAt asc
      """)
  List<Product> findAllForCafe(@Param("cafeId") UUID cafeId);

  @EntityGraph(attributePaths = "category")
  List<Product> findAllByCategoryIdOrderBySortOrderAscCreatedAtAsc(UUID categoryId);

  @Query("""
      select p from Product p
      where p.category.id in :categoryIds and p.active = true
      order by p.sortOrder asc, p.createdAt asc
      """)
  List<Product> findVisibleByCategoryIds(@Param("categoryIds") List<UUID> categoryIds);

  @EntityGraph(attributePaths = "category")
  Optional<Product> findBySlugAndCafeId(String slug, UUID cafeId);

  @Query("select p.category.id, count(p) from Product p where p.cafe.id = :cafeId group by p.category.id")
  List<Object[]> countByCategoryForCafe(@Param("cafeId") UUID cafeId);

  @Query("select coalesce(max(p.sortOrder), -1) from Product p where p.category.id = :categoryId")
  int findMaxSortOrder(@Param("categoryId") UUID categoryId);

  @Modifying(flushAutomatically = true)
  @Query("update Product p set p.sortOrder = p.sortOrder + 1 where p.category.id = :categoryId and p.sortOrder > :sortOrder")
  void shiftSortOrdersAfter(@Param("categoryId") UUID categoryId,
      @Param("sortOrder") int sortOrder);

  @Query("select p.imageUrl from Product p where p.imageUrl is not null")
  List<String> findAllImageUrls();

  boolean existsByCategoryId(UUID categoryId);

  boolean existsBySlug(String slug);
}
