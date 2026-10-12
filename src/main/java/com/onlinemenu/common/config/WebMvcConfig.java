package com.onlinemenu.common.config;

import com.onlinemenu.billing.service.BillingRequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final BillingRequestInterceptor billingRequestInterceptor;

  @Value("${app.upload.base-dir}")
  private String uploadBaseDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = uploadBaseDir.endsWith("/") ? uploadBaseDir : uploadBaseDir + "/";
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + location);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(billingRequestInterceptor)
        .addPathPatterns("/api/v1/**")
        .excludePathPatterns("/api/v1/public/**", "/api/v1/auth/**", "/api/v1/billing/**");
  }
}
