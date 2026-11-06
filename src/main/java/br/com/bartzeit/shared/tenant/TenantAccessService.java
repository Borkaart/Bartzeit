package br.com.bartzeit.shared.tenant;

import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessService {
    private final TenantResolver tenantResolver;

    public TenantAccessService(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    public TenantContext requireTenant(String requestedSlug, TenantUserPrincipal principal) {
        String normalized = tenantResolver.resolve(requestedSlug);
        if (principal == null || !principal.tenantSlug().equals(normalized)) {
            throw new ResourceNotFoundException("Tenant was not found.");
        }
        return new TenantContext(principal.tenantSlug(), principal.tenantId());
    }
}
