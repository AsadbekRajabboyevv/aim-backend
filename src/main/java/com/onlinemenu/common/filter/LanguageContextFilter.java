package com.onlinemenu.common.filter;

import com.onlinemenu.common.util.ActiveLanguageProvider;
import com.onlinemenu.common.util.LanguageContextHolder;
import com.onlinemenu.common.util.LanguageUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Runs before the Spring Security chain so 401/403 responses are localized too. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LanguageContextFilter extends OncePerRequestFilter {

  private final ActiveLanguageProvider activeLanguageProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain)
      throws ServletException, IOException {
    try {
      String requested = LanguageUtils.extractRequestedLanguage(request);
      String resolved = (requested != null && activeLanguageProvider.isActive(requested))
          ? requested
          : activeLanguageProvider.getDefaultLanguageCode();
      LanguageContextHolder.set(resolved);
      chain.doFilter(request, response);
    } finally {
      LanguageContextHolder.clear();
    }
  }
}
