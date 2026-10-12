package com.onlinemenu.common.constant;

public final class ExceptionMessageCodes {

  private ExceptionMessageCodes() {
  }

  public static final String GENERIC_ERROR = "error.generic";
  public static final String VALIDATION_FAILED = "error.validation.failed";

  public static final String USER_NOT_FOUND = "error.user.not_found";
  public static final String USER_EMAIL_ALREADY_EXISTS = "error.user.email_exists";

  public static final String CAFE_NOT_FOUND = "error.cafe.not_found";
  public static final String CAFE_SLUG_ALREADY_EXISTS = "error.cafe.slug_exists";
  public static final String CAFE_OWNER_INVALID = "error.cafe.owner_invalid";

  public static final String CATEGORY_NOT_FOUND = "error.category.not_found";
  public static final String CATEGORY_NOT_EMPTY = "error.category.not_empty";
  public static final String PRODUCT_NOT_FOUND = "error.product.not_found";
  public static final String MENU_CAFE_INACTIVE = "error.menu.cafe_inactive";

  public static final String DISCOUNT_NOT_FOUND = "error.discount.not_found";
  public static final String DISCOUNT_PRICE_INVALID = "error.discount.price_invalid";
  public static final String DISCOUNT_PERIOD_INVALID = "error.discount.period_invalid";
  public static final String DISCOUNT_OVERLAP = "error.discount.overlap";

  public static final String PROFILE_PASSWORD_INVALID = "error.profile.password_invalid";

  public static final String AI_DISABLED = "error.ai.disabled";
  public static final String AI_NOT_CONFIGURED = "error.ai.not_configured";
  public static final String AI_PROVIDER_FAILED = "error.ai.provider_failed";
  public static final String AI_RATE_LIMITED = "error.ai.rate_limited";
  public static final String AI_MODEL_UNAVAILABLE = "error.ai.model_unavailable";
  public static final String AI_PROVIDER_BUSY = "error.ai.provider_busy";
  public static final String AI_PROVIDER_UNKNOWN = "error.ai.provider_unknown";
  public static final String AI_CONVERSATION_NOT_FOUND = "error.ai.conversation_not_found";
  public static final String AI_ACTION_NOT_PENDING = "error.ai.action_not_pending";
  public static final String AI_TOOL_NOT_FOUND = "error.ai.tool_not_found";
  public static final String AI_TOOL_NAME_EXISTS = "error.ai.tool_name_exists";
  public static final String AI_TOOL_INVALID = "error.ai.tool_invalid";

  public static final String BILLING_LIMIT_REACHED = "error.billing.limit_reached";
  public static final String BILLING_FEATURE_UNAVAILABLE = "error.billing.feature_unavailable";
  public static final String BILLING_PLAN_NOT_FOUND = "error.billing.plan_not_found";
  public static final String BILLING_PLAN_IN_USE = "error.billing.plan_in_use";
  public static final String BILLING_PLAN_NOT_AVAILABLE = "error.billing.plan_not_available";
  public static final String BILLING_LIMIT_NOT_FOUND = "error.billing.limit_not_found";
  public static final String BILLING_LIMIT_KEY_EXISTS = "error.billing.limit_key_exists";
  public static final String BILLING_LIMIT_INVALID = "error.billing.limit_invalid";
  public static final String BILLING_LIMIT_SYSTEM = "error.billing.limit_system";
  public static final String BILLING_PROVIDER_UNAVAILABLE = "error.billing.provider_unavailable";
  public static final String BILLING_COUNTRY_REQUIRED = "error.billing.country_required";
  public static final String BILLING_CURRENCY_UNAVAILABLE = "error.billing.currency_unavailable";
  public static final String BILLING_PAYMENT_NOT_FOUND = "error.billing.payment_not_found";

  public static final String LANGUAGE_NOT_FOUND = "error.language.not_found";
  public static final String LANGUAGE_CODE_ALREADY_EXISTS = "error.language.code_exists";
  public static final String LANGUAGE_CANNOT_DELETE_DEFAULT = "error.language.cannot_delete_default";
  public static final String LANGUAGE_DEFAULT_REQUIRED = "error.language.default_required";

  public static final String SYSTEM_MESSAGE_NOT_FOUND = "error.system_message.not_found";
  public static final String SYSTEM_MESSAGE_CODE_ALREADY_EXISTS = "error.system_message.code_exists";

  public static final String LEGAL_DOCUMENT_NOT_FOUND = "error.legal.not_found";

  public static final String SETTING_NOT_FOUND = "error.setting.not_found";
  public static final String SETTING_INVALID_VALUE = "error.setting.invalid_value";
  public static final String SETTING_KEY_ALREADY_EXISTS = "error.setting.key_exists";
  public static final String SETTING_PROTECTED = "error.setting.protected";

  public static final String INVALID_CREDENTIALS = "error.auth.invalid_credentials";
  public static final String PASSWORD_NOT_SET = "error.auth.password_not_set";
  public static final String EMAIL_NOT_VERIFIED = "error.auth.email_not_verified";
  public static final String VERIFICATION_INVALID = "error.auth.verification_invalid";
  public static final String VERIFICATION_EXPIRED = "error.auth.verification_expired";
  public static final String VERIFICATION_ATTEMPTS_EXCEEDED = "error.auth.verification_attempts_exceeded";
  public static final String VERIFICATION_RESEND_TOO_SOON = "error.auth.verification_resend_too_soon";
  public static final String EMAIL_SEND_FAILED = "error.auth.email_send_failed";
  public static final String TELEGRAM_DISABLED = "error.auth.telegram_disabled";
  public static final String TELEGRAM_INVALID = "error.auth.telegram_invalid";
  public static final String TOKEN_EXPIRED = "error.auth.token_expired";
  public static final String TOKEN_INVALID = "error.auth.token_invalid";
  public static final String REFRESH_TOKEN_NOT_FOUND = "error.auth.refresh_token_not_found";
  public static final String REFRESH_TOKEN_REVOKED = "error.auth.refresh_token_revoked";
  public static final String ACCESS_DENIED = "error.auth.access_denied";

  public static final String FILE_EMPTY = "error.file.empty";
  public static final String FILE_TYPE_NOT_ALLOWED = "error.file.type_not_allowed";
  public static final String FILE_TOO_LARGE = "error.file.too_large";
  public static final String FILE_STORAGE_FAILED = "error.file.storage_failed";

  // Bean Validation message codes (used as @NotBlank(message = ...) etc.)
  public static final String VALIDATION_REQUIRED = "validation.required";
  public static final String VALIDATION_EMAIL_INVALID = "validation.email.invalid";
  public static final String VALIDATION_SIZE = "validation.size";
  public static final String VALIDATION_PASSWORD_SIZE = "validation.password.size";
  public static final String VALIDATION_PRICE_INVALID = "validation.price.invalid";
  public static final String VALIDATION_SETTING_KEY_FORMAT = "validation.setting.key_format";
  public static final String VALIDATION_URL_INVALID = "validation.url.invalid";
  public static final String VALIDATION_COUNTRY_INVALID = "validation.country.invalid";
  public static final String VALIDATION_VERIFICATION_CODE = "validation.verification_code";
}
