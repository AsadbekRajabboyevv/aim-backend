package com.onlinemenu.cafe.dto;

import com.onlinemenu.cafe.domain.MenuTheme;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CafeRequestDto(

    @NotEmpty(message = ExceptionMessageCodes.VALIDATION_REQUIRED)
    Map<String, String> nameTranslations,

    Map<String, String> descriptionTranslations,

    String logoUrl,
    String coverUrl,

    @Size(max = 50, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String phone,

    @Size(max = 500, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String address,

    @Email(message = ExceptionMessageCodes.VALIDATION_EMAIL_INVALID)
    @Size(max = 200, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String email,

    @Size(max = 200, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String workingHours,

    @Size(max = 100, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String wifiName,

    @Size(max = 100, message = ExceptionMessageCodes.VALIDATION_SIZE)
    String wifiPassword,

    Map<String, String> socialLinks,

    MenuTheme theme,

    boolean active,

    String ownerSlug
) {

}
