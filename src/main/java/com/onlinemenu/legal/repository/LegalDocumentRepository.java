package com.onlinemenu.legal.repository;

import com.onlinemenu.legal.domain.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

  Optional<LegalDocument> findBySlug(String slug);

  List<LegalDocument> findAllByOrderBySlugDesc();
}
