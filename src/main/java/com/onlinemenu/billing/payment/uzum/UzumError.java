package com.onlinemenu.billing.payment.uzum;

/**
 * Error codes of the Uzum Bank Merchant API. Check them against the documentation Uzum sends when
 * the merchant is connected.
 */
enum UzumError {
  ACCESS_DENIED("10001"),
  JSON_PARSING("10002"),
  INVALID_OPERATION("10003"),
  SERVICE_INVALID("10006"),
  ORDER_NOT_FOUND("10007"),
  ALREADY_PAID("10008"),
  WRONG_AMOUNT("10011"),
  TRANSACTION_NOT_FOUND("10014"),
  CANCELLED("10015"),
  CANNOT_REVERSE("10017");

  private final String code;

  UzumError(String code) {
    this.code = code;
  }

  String code() {
    return code;
  }
}
