package com.shop.platform.core;

/**
 * Base for every expected business failure. Carries a stable {@code code} (resolved to an i18n
 * message at the edge — never returned raw) and a {@link ProblemCategory} the web layer maps to
 * a {@code ProblemDetail} status. {@code messageArgs} feed the i18n {@code MessageSource}.
 */
public class DomainException extends RuntimeException {

	private final String code;
	private final ProblemCategory category;
	private final transient Object[] messageArgs;

	public DomainException(String code, ProblemCategory category, String developerMessage, Object... messageArgs) {
		super(developerMessage);
		this.code = code;
		this.category = category;
		this.messageArgs = messageArgs != null ? messageArgs.clone() : new Object[0];
	}

	public String code() {
		return code;
	}

	public ProblemCategory category() {
		return category;
	}

	public Object[] messageArgs() {
		return messageArgs.clone();
	}
}
