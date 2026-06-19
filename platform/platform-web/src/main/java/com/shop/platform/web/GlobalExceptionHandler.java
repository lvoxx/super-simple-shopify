package com.shop.platform.web;

import com.shop.platform.core.DomainException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The single edge for turning exceptions into RFC 9457 {@code ProblemDetail} responses. Every
 * error code is resolved to a localized message through {@link MessageSource} — raw error codes
 * never leave the process. This is the only {@code @RestControllerAdvice} in the system.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private final MessageSource messages;

	public GlobalExceptionHandler(MessageSource messages) {
		this.messages = messages;
	}

	@ExceptionHandler(DomainException.class)
	public ProblemDetail handleDomain(DomainException ex, Locale locale) {
		HttpStatus status = HttpStatus.valueOf(ex.category().canonicalStatus());
		String detail = messages.getMessage(ex.code(), ex.messageArgs(), ex.getMessage(), locale);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setProperty("code", ex.code());
		problem.setProperty("category", ex.category().name());
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex, Locale locale) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				messages.getMessage("validation.failed", null, "Validation failed", locale));
		problem.setProperty("code", "validation.failed");
		problem.setProperty("category", "VALIDATION");
		problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
				.map(this::describe).toList());
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex, Locale locale) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				messages.getMessage("internal.error", null, "Internal error", locale));
		problem.setProperty("code", "internal.error");
		problem.setProperty("category", "INTERNAL");
		return problem;
	}

	private String describe(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}
}
