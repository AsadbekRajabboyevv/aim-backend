package com.onlinemenu.common.exception;

import com.onlinemenu.auth.dto.VerificationSentDto;
import com.onlinemenu.auth.service.EmailNotVerifiedException;
import com.onlinemenu.billing.dto.PlanLimitErrorDto;
import com.onlinemenu.billing.service.PlanLimitExceededException;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.util.ExceptionUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final ExceptionUtils exceptionUtils;

  @ExceptionHandler(EmailNotVerifiedException.class)
  public ResponseEntity<BaseResponse<VerificationSentDto>> handleEmailNotVerified(
      EmailNotVerifiedException ex) {
    String message = exceptionUtils.resolveMessage(ex);
    return ResponseEntity.status(ex.getStatus())
        .body(BaseResponse.failure(message, ex.getDetails()));
  }

  @ExceptionHandler(PlanLimitExceededException.class)
  public ResponseEntity<BaseResponse<PlanLimitErrorDto>> handlePlanLimit(
      PlanLimitExceededException ex) {
    String message = exceptionUtils.resolveMessage(ex);
    return ResponseEntity.status(ex.getStatus())
        .body(BaseResponse.failure(message, ex.getDetails()));
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {
    String message = exceptionUtils.resolveMessage(ex);
    return ResponseEntity.status(ex.getStatus()).body(BaseResponse.error(message));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Void>> handleValidationException(
      MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(this::resolveFieldError)
        .toList();
    String message = exceptionUtils.resolveMessage(ExceptionMessageCodes.VALIDATION_FAILED);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BaseResponse.error(message, errors));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(
      ConstraintViolationException ex) {
    List<String> errors = ex.getConstraintViolations().stream()
        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
        .toList();
    String message = exceptionUtils.resolveMessage(ExceptionMessageCodes.VALIDATION_FAILED);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BaseResponse.error(message, errors));
  }

  @ExceptionHandler({AccessDeniedException.class})
  public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    String message = exceptionUtils.resolveMessage(ExceptionMessageCodes.ACCESS_DENIED);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(BaseResponse.error(message));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<BaseResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
    String message = exceptionUtils.resolveMessage(ExceptionMessageCodes.INVALID_CREDENTIALS);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(BaseResponse.error(message));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<BaseResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
    String message = exceptionUtils.resolveMessage(ExceptionMessageCodes.FILE_TOO_LARGE);
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(BaseResponse.error(message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleGenericException(Exception ex) {
    log.error("Unhandled exception", ex);
    String message = exceptionUtils.resolveMessage(ExceptionMessageCodes.GENERIC_ERROR);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(BaseResponse.error(message));
  }

  private String resolveFieldError(FieldError fieldError) {
    String resolved = exceptionUtils.resolveMessage(fieldError.getDefaultMessage());
    return fieldError.getField() + ": " + resolved;
  }
}
