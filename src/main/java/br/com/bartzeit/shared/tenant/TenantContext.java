package br.com.bartzeit.shared.tenant;

import java.util.UUID;

public record TenantContext(String slug, UUID barbershopId) {
}
