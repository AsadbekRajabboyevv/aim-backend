package com.onlinemenu.menu.controller;

import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.menu.dto.MenuLinkDto;
import com.onlinemenu.menu.dto.PublicMenuResponseDto;
import com.onlinemenu.menu.service.PublicMenuService;
import com.onlinemenu.menu.service.QrCodeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Validated
@RestController
@RequestMapping("/api/v1/public/menu")
@RequiredArgsConstructor
public class PublicMenuController {

  private static final String SVG_MEDIA_TYPE = "image/svg+xml";

  private final PublicMenuService publicMenuService;
  private final QrCodeService qrCodeService;

  @GetMapping("/{cafeSlug}")
  public BaseResponse<PublicMenuResponseDto> get(@PathVariable String cafeSlug) {
    return BaseResponse.success(publicMenuService.get(cafeSlug));
  }

  @GetMapping("/{cafeSlug}/link")
  public BaseResponse<MenuLinkDto> link(@PathVariable String cafeSlug,
      @RequestParam(required = false) @Min(1) @Max(9999) Integer table) {
    return BaseResponse.success(qrCodeService.link(cafeSlug, table));
  }

  @GetMapping(value = "/{cafeSlug}/qr.svg", produces = SVG_MEDIA_TYPE)
  public ResponseEntity<String> qr(@PathVariable String cafeSlug,
      @RequestParam(required = false) @Min(1) @Max(9999) Integer table) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
        .body(qrCodeService.svg(cafeSlug, table));
  }
}
