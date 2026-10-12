package com.onlinemenu.systemmessage.repository;

import com.onlinemenu.systemmessage.domain.SystemMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemMessageRepository extends JpaRepository<SystemMessage, UUID> {

  @Query(value = """
      select * from system_message sm
      where (cast(:search as text) is null or lower(sm.code) like lower(concat('%', cast(:search as text), '%')))
        and (cast(:prefix as text) is null or sm.code like concat(cast(:prefix as text), '%'))
        and (cast(:missingLanguage as text) is null
             or coalesce(trim(sm.translations ->> cast(:missingLanguage as text)), '') = '')
      """,
      countQuery = """
      select count(*) from system_message sm
      where (cast(:search as text) is null or lower(sm.code) like lower(concat('%', cast(:search as text), '%')))
        and (cast(:prefix as text) is null or sm.code like concat(cast(:prefix as text), '%'))
        and (cast(:missingLanguage as text) is null
             or coalesce(trim(sm.translations ->> cast(:missingLanguage as text)), '') = '')
      """,
      nativeQuery = true)
  Page<SystemMessage> search(@Param("search") String search, @Param("prefix") String prefix,
      @Param("missingLanguage") String missingLanguage,
      Pageable pageable);

  Optional<SystemMessage> findByCode(String code);

  List<SystemMessage> findAllByCodeStartingWith(String prefix);

  boolean existsByCode(String code);
}
