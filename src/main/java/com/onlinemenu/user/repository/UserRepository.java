package com.onlinemenu.user.repository;

import com.onlinemenu.user.domain.Role;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.dto.UserFilter;
import com.onlinemenu.user.dto.UserResponseDto;
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

public interface UserRepository extends JpaRepository<User, UUID> {

  @Query("""
      select new com.onlinemenu.user.dto.UserResponseDto(
          u.slug, u.fullName, u.email, u.role,
          (select count(c) from Cafe c where c.owner = u),
          u.provider, u.active, u.createdAt)
      from User u
      where (:#{#filter.search()} is null
              or lower(u.fullName) like lower(concat('%', :#{#filter.search()}, '%'))
              or lower(u.email) like lower(concat('%', :#{#filter.search()}, '%')))
        and (:#{#filter.role()} is null or u.role = :#{#filter.role()})
        and (:#{#filter.cafeSlug()} is null
              or exists (select 1 from Cafe c where c.owner = u and c.slug = :#{#filter.cafeSlug()}))
        and (:#{#filter.active()} is null or u.active = :#{#filter.active()})
      """)
  Page<UserResponseDto> search(@Param("filter") UserFilter filter, Pageable pageable);

  Optional<User> findBySlug(String slug);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.slug = :slug")
  Optional<User> findBySlugForUpdate(@Param("slug") String slug);

  Optional<User> findByEmail(String email);

  Optional<User> findByTelegramId(Long telegramId);

  List<User> findAllByRoleOrderByFullNameAsc(Role role);

  @Query("select count(c) from Cafe c where c.owner.id = :userId")
  long countOwnedCafes(@Param("userId") UUID userId);

  boolean existsBySlug(String slug);

  boolean existsByEmail(String email);
}
