package com.onlinemenu.ai.tool.repository;

import com.onlinemenu.ai.tool.domain.AiTool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiToolRepository extends JpaRepository<AiTool, UUID> {

  @Query("""
      select t from AiTool t
      where :search = ''
         or lower(t.name) like lower(concat('%', :search, '%'))
         or lower(t.title) like lower(concat('%', :search, '%'))
      """)
  Page<AiTool> search(@Param("search") String search, Pageable pageable);

  List<AiTool> findAllByActiveTrueOrderByNameAsc();

  Optional<AiTool> findByName(String name);

  boolean existsByName(String name);
}
