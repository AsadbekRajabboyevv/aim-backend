package com.onlinemenu.security.jwt;

import com.onlinemenu.common.constant.SecurityConstants;
import com.onlinemenu.common.util.JwtUtils;
import com.onlinemenu.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtUtils jwtUtils;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain)
      throws ServletException, IOException {
    String token = extractToken(request);
    if (StringUtils.hasText(token) && jwtUtils.isValid(token)
        && SecurityConstants.TOKEN_TYPE_ACCESS.equals(jwtUtils.extractTokenType(token))) {
      UserPrincipal principal = UserPrincipal.fromJwtClaims(jwtUtils.extractUserClaims(token));
      var authentication = new UsernamePasswordAuthenticationToken(principal, null,
          principal.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    chain.doFilter(request, response);
  }

  private String extractToken(HttpServletRequest request) {
    String header = request.getHeader(SecurityConstants.AUTH_HEADER);
    if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
      return header.substring(SecurityConstants.TOKEN_PREFIX.length());
    }
    return null;
  }
}
