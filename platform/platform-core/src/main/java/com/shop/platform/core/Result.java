package com.shop.platform.core;

import java.util.Objects;
import java.util.function.Function;

/**
 * A success-or-failure outcome without exceptions on the happy path. Failures carry a stable
 * {@code code} (resolved to an i18n message at the edge) plus a developer-facing message.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

	record Success<T>(T value) implements Result<T> {
		public Success {
			Objects.requireNonNull(value, "value");
		}
	}

	record Failure<T>(String code, String message) implements Result<T> {
		public Failure {
			Objects.requireNonNull(code, "code");
		}
	}

	static <T> Result<T> success(T value) {
		return new Success<>(value);
	}

	static <T> Result<T> failure(String code, String message) {
		return new Failure<>(code, message);
	}

	default boolean isSuccess() {
		return this instanceof Success<T>;
	}

	default <R> Result<R> map(Function<? super T, ? extends R> mapper) {
		return switch (this) {
			case Success<T> s -> Result.success(mapper.apply(s.value()));
			case Failure<T> f -> Result.failure(f.code(), f.message());
		};
	}
}
