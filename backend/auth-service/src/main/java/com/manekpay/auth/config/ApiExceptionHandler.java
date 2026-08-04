package com.manekpay.auth.config;

import com.manekpay.auth.dto.ErrorResponse;
import com.manekpay.auth.exception.DuplicateEmailException;
import com.manekpay.auth.exception.InvalidCredentialsException;
import com.manekpay.auth.exception.InvalidTokenException;
import com.manekpay.auth.exception.ForbiddenInquiryAccessException;
import com.manekpay.auth.exception.InquiryNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(InquiryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInquiryNotFound(InquiryNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenInquiryAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenInquiryAccessException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // ResponseEntityExceptionHandler.handleException(...) is itself @ExceptionHandler-annotated for
    // MethodArgumentNotValidException (among ~18 built-in framework exception types), so a plain
    // @ExceptionHandler(MethodArgumentNotValidException.class) method here would collide with it and
    // fail at startup with "Ambiguous @ExceptionHandler method mapped". Override the specific hook
    // ResponseEntityExceptionHandler provides for this exception instead, to keep the field-level
    // message extraction this app relies on.
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
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
