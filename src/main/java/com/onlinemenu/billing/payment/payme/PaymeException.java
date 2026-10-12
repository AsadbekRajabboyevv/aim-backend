package com.onlinemenu.billing.payment.payme;

class PaymeException extends RuntimeException {

  private final PaymeError error;
  private final String data;

  PaymeException(PaymeError error, String data) {
    super(error.name());
    this.error = error;
    this.data = data;
  }

  PaymeError error() {
    return error;
  }

  String data() {
    return data;
  }
}
