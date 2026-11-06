package br.com.bartzeit.tenants.api;

import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.shared.tenant.TenantAccessService;
import br.com.bartzeit.shared.tenant.TenantContext;
import br.com.bartzeit.tenants.application.BarbershopService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/barbershops")
public class BarbershopController {
    private final BarbershopService barbershopService;
    private final TenantAccessService tenantAccessService;

    public BarbershopController(BarbershopService barbershopService, TenantAccessService tenantAccessService) {
        this.barbershopService = barbershopService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public BarbershopService.BarbershopResponse getCurrentTenant(
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        TenantContext tenant = new TenantContext(principal.tenantSlug(), principal.tenantId());
        return barbershopService.getCurrentTenant(tenant);
    }

    @GetMapping("/{tenantSlug}")
    public BarbershopService.BarbershopResponse getBySlug(
            @PathVariable String tenantSlug,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return barbershopService.getCurrentTenant(tenantAccessService.requireTenant(tenantSlug, principal));
    }
}
