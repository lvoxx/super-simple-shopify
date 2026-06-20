package com.shop.store;

/**
 * Request to provision a new shop. A control-plane operation: it allocates the shop id and its shard.
 * Edge validation (non-blank name, well-formed domain) happens on the inbound request DTO before this
 * command is built — the API command itself stays a plain carrier (depends only on platform-core).
 *
 * @param name          merchant-facing shop name
 * @param plan          subscription tier; defaults applied by the caller if null
 * @param locale        BCP-47 default locale (e.g. {@code en-US})
 * @param primaryDomain the shop's primary host (e.g. {@code acme.myshop.example}); used for storefront
 *                      tenant resolution and must be globally unique
 */
public record CreateShopCommand(String name, ShopPlan plan, String locale, String primaryDomain) {
}
