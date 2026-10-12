package com.onlinemenu.cafe.repository;

import com.onlinemenu.cafe.domain.Cafe;
import com.onlinemenu.cafe.dto.CafeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CafeRepository extends JpaRepository<Cafe, UUID> {

  @EntityGraph(attributePaths = "owner")
  @Query(value = """
      select c from Cafe c left join c.owner o
      where (:#{#filter.search()} is null
              or lower(c.slug) like lower(concat('%', :#{#filter.search()}, '%'))
              or lower(c.phone) like lower(concat('%', :#{#filter.search()}, '%'))
              or lower(c.address) like lower(concat('%', :#{#filter.search()}, '%')))
        and (:#{#filter.active()} is null or c.active = :#{#filter.active()})
        and (:#{#filter.ownerSlug()} is null or o.slug = :#{#filter.ownerSlug()})
      """,
      countQuery = """
          select count(c) from Cafe c left join c.owner o
          where (:#{#filter.search()} is null
                  or lower(c.slug) like lower(concat('%', :#{#filter.search()}, '%'))
                  or lower(c.phone) like lower(concat('%', :#{#filter.search()}, '%'))
                  or lower(c.address) like lower(concat('%', :#{#filter.search()}, '%')))
            and (:#{#filter.active()} is null or c.active = :#{#filter.active()})
            and (:#{#filter.ownerSlug()} is null or o.slug = :#{#filter.ownerSlug()})
          """)
  Page<Cafe> search(@Param("filter") CafeFilter filter, Pageable pageable);

  @EntityGraph(attributePaths = "owner")
  Optional<Cafe> findBySlug(String slug);

  @EntityGraph(attributePaths = "owner")
  List<Cafe> findAllByOwnerSlugOrderByCreatedAtAsc(String ownerSlug);

  long countByOwnerId(UUID ownerId);

  @Query("select c.logoUrl from Cafe c where c.logoUrl is not null")
  List<String> findAllLogoUrls();

  @Query("select c.coverUrl from Cafe c where c.coverUrl is not null")
  List<String> findAllCoverUrls();

  boolean existsBySlug(String slug);

  boolean existsBySlugAndOwnerSlug(String slug, String ownerSlug);
}
