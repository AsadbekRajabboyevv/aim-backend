package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.LimitDefinition;
import com.onlinemenu.security.UserPrincipal;
import com.onlinemenu.user.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Enforces limits that have a path pattern, so a new limit added in the admin panel works without
 * code: a FEATURE that is off blocks matching requests, a METERED one counts each successful
 * request and blocks once the period's allowance is used.
 */
@Component
@RequiredArgsConstructor
public class BillingRequestInterceptor implements HandlerInterceptor {

  private static final String COUNT_ATTRIBUTE = BillingRequestInterceptor.class.getName() + ".count";

  private final LimitDefinitionService limitDefinitionService;
  private final EntitlementService entitlementService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    UserPrincipal principal = cafeAdmin();
    if (principal == null) {
      return true;
    }
    String path = request.getRequestURI().substring(request.getContextPath().length());
    List<LimitDefinition> matching = limitDefinitionService.matching(request.getMethod(), path);
    if (!matching.isEmpty()) {
      request.setAttribute(COUNT_ATTRIBUTE,
          entitlementService.checkRequest(principal.getId(), matching));
    }
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    Object toCount = request.getAttribute(COUNT_ATTRIBUTE);
    UserPrincipal principal = cafeAdmin();
    if (toCount instanceof List<?> definitions && !definitions.isEmpty() && principal != null
        && ex == null && response.getStatus() < 400) {
      entitlementService.recordUse(principal.getId(), (List<LimitDefinition>) definitions);
    }
  }

  private static UserPrincipal cafeAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        && Role.CAFE_ADMIN.name().equals(principal.getRole())) {
      return principal;
    }
    return null;
  }
}
