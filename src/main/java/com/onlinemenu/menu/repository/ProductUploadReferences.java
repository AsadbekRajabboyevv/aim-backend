package com.onlinemenu.menu.repository;

import com.onlinemenu.common.util.UploadReferenceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class ProductUploadReferences implements UploadReferenceProvider {

  private final ProductRepository productRepository;

  @Override
  @Transactional(readOnly = true)
  public Collection<String> referencedUploadUrls() {
    return productRepository.findAllImageUrls();
  }
}
