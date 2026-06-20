/**
 * The {@code identity} bounded context — the system of record for a shop's <strong>staff users</strong>
 * and their RBAC roles. It answers "who, within this shop, may do what", which the admin surface and
 * other modules consult for authorization decisions.
 *
 * <p>This module does <strong>not</strong> authenticate: token issuance and validation are delegated to
 * Keycloak at the gateway, and the modules are never their own OAuth2 resource servers. A
 * {@link com.shop.identity.StaffUserView staff user} maps a Keycloak {@code subject} (the {@code sub}
 * claim) to a per-shop identity plus a set of {@link com.shop.identity.StaffRole roles}.
 *
 * <p>Staff users are <strong>tenant</strong> data — sharded and indexed by {@code shop_id}; every read is
 * tenant-scoped via {@code TenantContext}. This package is the module's public API (records, the
 * {@link com.shop.identity.StaffInvited} event, and the {@link com.shop.identity.IdentityFacade}
 * interface); the implementation lives in {@code com.shop.identity.internal}.
 */
package com.shop.identity;
