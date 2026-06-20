package com.shop.platform.security;

import com.shop.platform.core.DomainException;
import com.shop.platform.core.ProblemCategory;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the authenticated {@link Principal} for the dynamic scope of a request, using a
 * {@link ScopedValue} (final in JDK 25) — the security-side sibling of {@code TenantContext}. The filter
 * binds it from the identity the gateway forwards; module code reads it for RBAC decisions.
 *
 * <p>Storefront (anonymous) requests bind a tenant but <strong>no</strong> principal, so
 * {@link #currentPrincipal()} is empty there. {@link #requirePrincipal()} is a hard failure when a path
 * that needs an authenticated caller has none — never a silent anonymous fallback.
 */
public final class SecurityContext {

	private static final ScopedValue<Principal> CURRENT = ScopedValue.newInstance();

	private SecurityContext() {
	}

	/** Work that may throw, run inside a principal scope. */
	@FunctionalInterface
	public interface PrincipalWork<R> {
		R run() throws Exception;
	}

	/** Bind {@code principal} for the duration of {@code work} (value-returning, may throw). */
	public static <R> R callWithPrincipal(Principal principal, PrincipalWork<R> work) throws Exception {
		Objects.requireNonNull(principal, "principal");
		return ScopedValue.where(CURRENT, principal).call(work::run);
	}

	public static Optional<Principal> currentPrincipal() {
		return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
	}

	/** The authenticated principal, or a hard 401 if the request bound none. */
	public static Principal requirePrincipal() {
		if (!CURRENT.isBound()) {
			throw new DomainException("security.unauthenticated", ProblemCategory.UNAUTHENTICATED,
					"No authenticated principal bound to the request");
		}
		return CURRENT.get();
	}
}
