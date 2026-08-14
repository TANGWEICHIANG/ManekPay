package com.manekpay.ledger.config;

import com.manekpay.ledger.dto.ErrorResponse;
import com.manekpay.ledger.exception.AccountRestrictedException;
import com.manekpay.ledger.exception.AuthServiceUnavailableException;
import com.manekpay.ledger.exception.DuplicateProxyException;
import com.manekpay.ledger.exception.FxServiceUnavailableException;
import com.manekpay.ledger.exception.InsufficientBalanceException;
import com.manekpay.ledger.exception.KycNotApprovedException;
import com.manekpay.ledger.exception.ProxyNotFoundException;
import com.manekpay.ledger.exception.RecipientNotFoundException;
import com.manekpay.ledger.exception.RiskServiceUnavailableException;
import com.manekpay.ledger.exception.SelfTransferException;
import com.manekpay.ledger.exception.TransferNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DuplicateProxyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProxy(DuplicateProxyException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ProxyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProxyNotFound(ProxyNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(RecipientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRecipientNotFound(RecipientNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransferNotFound(TransferNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(SelfTransferException.class)
    public ResponseEntity<ErrorResponse> handleSelfTransfer(SelfTransferException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(KycNotApprovedException.class)
    public ResponseEntity<ErrorResponse> handleKycNotApproved(KycNotApprovedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAuthServiceUnavailable(AuthServiceUnavailableException ex, HttpServletRequest request) {
        log.error("auth-service unreachable on {} {}", request.getMethod(), request.getRequestURI(), ex.getCause());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(FxServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleFxServiceUnavailable(FxServiceUnavailableException ex, HttpServletRequest request) {
        log.error("fx-service unreachable on {} {}", request.getMethod(), request.getRequestURI(), ex.getCause());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountRestrictedException.class)
    public ResponseEntity<ErrorResponse> handleAccountRestricted(AccountRestrictedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(RiskServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleRiskServiceUnavailable(RiskServiceUnavailableException ex, HttpServletRequest request) {
        log.error("risk-service unreachable on {} {}", request.getMethod(), request.getRequestURI(), ex.getCause());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    // Backstop for unique-constraint races that slip past an in-application pre-check (e.g. two
    // concurrent POST /transfers with the same X-Idempotency-Key both passing the Redis cache-miss
    // check before either commits - the DB's unique(idempotency_key) constraint, V4 migration,
    // is what actually prevents the double-debit; this just turns the loser's raw insert failure
    // into a sane response instead of an opaque 500).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "A conflicting request was already processed - please retry", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                    HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Validation failed"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(),
                HttpStatus.valueOf(status.value()).getReasonPhrase(), message, path);
        return new ResponseEntity<>(body, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                               HttpStatusCode status, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorBody = new ErrorResponse(Instant.now(), status.value(),
                HttpStatus.valueOf(status.value()).getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : HttpStatus.valueOf(status.value()).getReasonPhrase(),
                path);
        return new ResponseEntity<>(errorBody, headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
