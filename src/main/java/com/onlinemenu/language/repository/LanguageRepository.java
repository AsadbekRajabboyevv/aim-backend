package com.onlinemenu.language.repository;

import com.onlinemenu.language.domain.Language;
import com.onlinemenu.language.dto.LanguageFilter;
import com.onlinemenu.language.dto.LanguageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LanguageRepository extends JpaRepository<Language, UUID> {

  @Query("""
      select new com.onlinemenu.language.dto.LanguageResponseDto(
          l.code, l.name, l.nativeName, l.isDefault, l.isActive, l.sortOrder)
      from Language l
      where (:#{#filter.search()} is null
              or lower(l.name) like lower(concat('%', :#{#filter.search()}, '%'))
              or lower(l.code) like lower(concat('%', :#{#filter.search()}, '%')))
        and (:#{#filter.active()} is null or l.isActive = :#{#filter.active()})
      """)
  Page<LanguageResponseDto> search(@Param("filter") LanguageFilter filter, Pageable pageable);

  Optional<Language> findByCode(String code);

  boolean existsByCode(String code);

  List<Language> findAllByIsActiveTrueOrderBySortOrderAsc();

  Optional<Language> findByIsDefaultTrue();
}
