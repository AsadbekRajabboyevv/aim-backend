package com.onlinemenu.billing.service;

import com.onlinemenu.billing.domain.Payment;
import com.onlinemenu.billing.domain.PaymentStatus;
import com.onlinemenu.billing.dto.BillingMapper;
import com.onlinemenu.billing.dto.PaymentResponseDto;
import com.onlinemenu.billing.repository.PaymentRepository;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order state changes. Payment providers only translate their protocol; every status change goes
 * through {@link #markPaid} and {@link #cancel}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

  private static final int HISTORY_LIMIT = 100;

  private final PaymentRepository paymentRepository;
  private final SubscriptionService subscriptionService;
  private final UserRepository userRepository;
  private final BillingMapper mapper;

  public List<PaymentResponseDto> get(UUID userId) {
    return paymentRepository.findAllByUserIdOrderByCreatedAtDesc(userId,
            PageRequest.of(0, HISTORY_LIMIT)).stream()
        .map(payment -> mapper.toDto(payment, null)).toList();
  }

  public PaymentResponseDto get(UUID userId, long orderNumber) {
    return paymentRepository.findByOrderNumberAndUserId(orderNumber, userId)
        .map(payment -> mapper.toDto(payment, null))
        .orElseThrow(() -> new ResourceNotFoundException(
            ExceptionMessageCodes.BILLING_PAYMENT_NOT_FOUND, orderNumber));
  }

  public Page<PaymentResponseDto> search(PaymentStatus status, String provider,
      Pageable pageable) {
    Page<Payment> payments = paymentRepository.search(status,
        StringUtils.hasText(provider) ? provider : null, pageable);
    Map<UUID, String> emails = userRepository.findAllById(
            payments.map(Payment::getUserId).toSet()).stream()
        .collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a));
    return payments.map(payment -> mapper.toDto(payment, emails.get(payment.getUserId())));
  }

  /** Marks the order paid and moves the subscription; does nothing if it is already paid. */
  @Transactional
  public void markPaid(Payment payment) {
    if (payment.getStatus() == PaymentStatus.PAID) {
      return;
    }
    payment.setStatus(PaymentStatus.PAID);
    payment.setPaidAt(Instant.now());
    paymentRepository.save(payment);
    subscriptionService.applyPayment(payment);
  }

  @Transactional
  public void cancel(Payment payment, Integer reason) {
    if (payment.getStatus() != PaymentStatus.PENDING) {
      return;
    }
    payment.setStatus(PaymentStatus.CANCELLED);
    payment.setCancelledAt(Instant.now());
    payment.setCancelReason(reason);
    paymentRepository.save(payment);
  }

  /** Locks the order row for the rest of the transaction. */
  @Transactional
  public Optional<Payment> lockByOrder(String orderNumber) {
    try {
      return paymentRepository.findByOrderNumberForUpdate(Long.parseLong(orderNumber.trim()));
    } catch (NumberFormatException | NullPointerException ex) {
      return Optional.empty();
    }
  }

  @Transactional
  public Optional<Payment> lockByTransaction(String provider, String transactionId) {
    return paymentRepository.findByProviderTransactionForUpdate(provider, transactionId);
  }

  @Transactional
  public Payment save(Payment payment) {
    return paymentRepository.save(payment);
  }

  public List<Payment> byProviderTime(String provider, long from, long to) {
    return paymentRepository.findAllByProviderTimeBetween(provider, from, to);
  }
}
