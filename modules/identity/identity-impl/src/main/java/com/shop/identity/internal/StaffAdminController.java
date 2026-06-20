package com.shop.identity.internal;

import com.shop.identity.IdentityFacade;
import com.shop.identity.InviteStaffCommand;
import com.shop.identity.StaffRole;
import com.shop.identity.StaffUserView;
import com.shop.platform.core.DomainException;
import com.shop.platform.core.ProblemCategory;
import com.shop.platform.web.ApiVersion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin surface for managing a shop's staff. These endpoints are <strong>tenant-scoped</strong>: they
 * live under {@link ApiVersion#V1_PREFIX} (not the control prefix), so the tenant-binding filter resolves
 * {@code X-Shop-Id} and binds {@code TenantContext} before the service runs. In the deployed topology the
 * gateway + Keycloak authenticate the caller and forward identity; the module is not its own resource
 * server.
 */
@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/staff")
public class StaffAdminController {

	private final IdentityFacade identity;

	public StaffAdminController(IdentityFacade identity) {
		this.identity = identity;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public StaffUserView invite(@Valid @RequestBody InviteStaffRequest request) {
		return identity.invite(new InviteStaffCommand(
				request.subject(), request.displayName(), request.email(), request.roles()));
	}

	@GetMapping("/{subject}")
	public StaffUserView get(@PathVariable String subject) {
		return identity.findBySubject(subject)
				.orElseThrow(() -> new DomainException("identity.staff.not_found", ProblemCategory.NOT_FOUND,
						"No staff user with subject " + subject));
	}

	/** Inbound request DTO — the only place edge validation is declared. */
	public record InviteStaffRequest(
			@NotBlank String subject,
			@NotBlank String displayName,
			@NotBlank @Email String email,
			@NotEmpty Set<StaffRole> roles) {
	}
}
