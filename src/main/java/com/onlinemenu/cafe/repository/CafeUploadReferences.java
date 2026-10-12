package com.onlinemenu.cafe.repository;

import com.onlinemenu.common.util.UploadReferenceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CafeUploadReferences implements UploadReferenceProvider {

  private final CafeRepository cafeRepository;

  @Override
  @Transactional(readOnly = true)
  public Collection<String> referencedUploadUrls() {
    List<String> urls = new ArrayList<>(cafeRepository.findAllLogoUrls());
    urls.addAll(cafeRepository.findAllCoverUrls());
    return urls;
  }
}
