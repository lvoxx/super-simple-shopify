package com.shop.inventory;

/**
 * Command to create a stock location in the current shop. The target shop is the active tenant
 * ({@code TenantContext}), never a field here.
 */
public record CreateLocationCommand(String name) {
}
