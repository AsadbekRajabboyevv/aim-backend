package com.onlinemenu.billing.payment.payme;

import java.util.Map;

enum PaymeError {
  PARSE_ERROR(-32700, "JSON xato", "Ошибка разбора JSON", "Parse error"),
  METHOD_NOT_FOUND(-32601, "Metod topilmadi", "Метод не найден", "Method not found"),
  INSUFFICIENT_PRIVILEGE(-32504, "Ruxsat yo'q", "Недостаточно привилегий", "Insufficient privilege"),
  WRONG_AMOUNT(-31001, "Summa noto'g'ri", "Неверная сумма", "Wrong amount"),
  TRANSACTION_NOT_FOUND(-31003, "Tranzaksiya topilmadi", "Транзакция не найдена",
      "Transaction not found"),
  CANNOT_CANCEL(-31007, "Buyurtma bajarilgan, bekor qilib bo'lmaydi",
      "Заказ выполнен, отмена невозможна", "The order is completed and cannot be cancelled"),
  CANNOT_PERFORM(-31008, "Amalni bajarib bo'lmaydi", "Невозможно выполнить операцию",
      "Unable to perform the operation"),
  ORDER_NOT_FOUND(-31050, "Buyurtma topilmadi", "Заказ не найден", "Order not found"),
  ORDER_NOT_PAYABLE(-31051, "Buyurtma to'langan yoki bekor qilingan",
      "Заказ уже оплачен или отменён", "The order is already paid or cancelled"),
  ORDER_BUSY(-31099, "Buyurtma boshqa to'lovni kutmoqda", "Заказ ожидает другую оплату",
      "The order is waiting for another payment");

  private final int code;
  private final Map<String, String> message;

  PaymeError(int code, String uz, String ru, String en) {
    this.code = code;
    this.message = Map.of("uz", uz, "ru", ru, "en", en);
  }

  int code() {
    return code;
  }

  Map<String, String> message() {
    return message;
  }
}
