package com.onlinemenu.discount.repository;

import com.onlinemenu.discount.domain.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {

  @Query(value = """
      select d from Discount d join fetch d.cafe c join fetch d.product p left join c.owner o
      where (:search = ''
              or lower(p.slug) like lower(concat('%', :search, '%'))
              or lower(c.slug) like lower(concat('%', :search, '%')))
        and (:cafeSlug = '' or c.slug = :cafeSlug)
        and (:ownerSlug = '' or o.slug = :ownerSlug)
        and (:status = ''
              or (:status = 'DISABLED' and d.active = false)
              or (:status = 'ACTIVE' and d.active = true
                  and (d.startsAt is null or d.startsAt <= :now) and (d.endsAt is null or d.endsAt > :now))
              or (:status = 'SCHEDULED' and d.active = true and d.startsAt > :now)
              or (:status = 'EXPIRED' and d.active = true and d.endsAt <= :now))
      """,
      countQuery = """
          select count(d) from Discount d join d.cafe c join d.product p left join c.owner o
          where (:search = ''
                  or lower(p.slug) like lower(concat('%', :search, '%'))
                  or lower(c.slug) like lower(concat('%', :search, '%')))
            and (:cafeSlug = '' or c.slug = :cafeSlug)
            and (:ownerSlug = '' or o.slug = :ownerSlug)
            and (:status = ''
                  or (:status = 'DISABLED' and d.active = false)
                  or (:status = 'ACTIVE' and d.active = true
                      and (d.startsAt is null or d.startsAt <= :now) and (d.endsAt is null or d.endsAt > :now))
                  or (:status = 'SCHEDULED' and d.active = true and d.startsAt > :now)
                  or (:status = 'EXPIRED' and d.active = true and d.endsAt <= :now))
          """)
  Page<Discount> search(String search,
      String cafeSlug,
      String ownerSlug,
      String status,
      Instant now,
      Pageable pageable);

  @EntityGraph(attributePaths = {"cafe", "product"})
  Optional<Discount> findBySlugAndCafeId(String slug, UUID cafeId);

  @Query("""
      select d from Discount d
      where d.cafe.id = :cafeId and d.active = true
        and (d.startsAt is null or d.startsAt <= :now)
        and (d.endsAt is null or d.endsAt > :now)
      """)
  List<Discount> findRunningForCafe(UUID cafeId, Instant now);

  List<Discount> findAllByProductIdAndActiveTrue(UUID productId);

  boolean existsBySlug(String slug);
}
