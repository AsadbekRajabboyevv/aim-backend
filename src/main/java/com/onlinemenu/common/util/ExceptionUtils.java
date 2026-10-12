package com.onlinemenu.common.util;

import com.onlinemenu.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExceptionUtils {

  private final MessageResolver messageResolver;

  public String resolveMessage(BusinessException exception) {
    return resolveMessage(exception.getMessageCode(), exception.getArgs());
  }

  public String resolveMessage(String code, Object... args) {
    return messageResolver.resolve(code, LanguageContextHolder.get(), args);
  }
}
