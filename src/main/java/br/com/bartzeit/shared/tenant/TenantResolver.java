package br.com.bartzeit.shared.tenant;

import br.com.bartzeit.shared.error.InvalidTenantException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TenantResolver {

    public String resolve(String tenantSlug) {
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new InvalidTenantException("Tenant slug is required.");
        }

        String normalized = tenantSlug.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new InvalidTenantException("Tenant slug contains invalid characters.");
        }

        return normalized;
    }
}
