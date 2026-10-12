package com.onlinemenu.setting.repository;

import com.onlinemenu.setting.domain.Setting;
import com.onlinemenu.setting.dto.SettingFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SettingRepository extends JpaRepository<Setting, UUID> {

  @Query("""
      select s from Setting s
      where (:#{#filter.search()} is null
              or lower(s.key) like lower(concat('%', :#{#filter.search()}, '%'))
              or lower(s.description) like lower(concat('%', :#{#filter.search()}, '%')))
      """)
  Page<Setting> search(SettingFilter filter, Pageable pageable);

  Optional<Setting> findByKey(String key);

  boolean existsByKey(String key);
}
