package br.com.bartzeit.tenants.application;

import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantContext;
import br.com.bartzeit.tenants.domain.Barbershop;
import br.com.bartzeit.tenants.infrastructure.BarbershopRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BarbershopService {
    private final BarbershopRepository barbershopRepository;

    public BarbershopService(BarbershopRepository barbershopRepository) {
        this.barbershopRepository = barbershopRepository;
    }

    public BarbershopResponse getCurrentTenant(TenantContext tenantContext) {
        return barbershopRepository.findById(tenantContext.barbershopId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant was not found."));
    }

    public Barbershop getRequiredEntity(TenantContext tenantContext) {
        return barbershopRepository.findById(tenantContext.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant was not found."));
    }

    public BarbershopResponse toResponse(Barbershop barbershop) {
        return new BarbershopResponse(
                barbershop.getId(),
                barbershop.getName(),
                barbershop.getSlug(),
                barbershop.getCity(),
                barbershop.getAddress(),
                barbershop.getPhone(),
                barbershop.isActive()
        );
    }

    public record BarbershopResponse(
            UUID id,
            String name,
            String slug,
            String city,
            String address,
            String phone,
            boolean active
    ) {
    }
}
