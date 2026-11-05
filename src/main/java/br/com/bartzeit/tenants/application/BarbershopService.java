package br.com.bartzeit.tenants.application;

import br.com.bartzeit.shared.error.ConflictException;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantResolver;
import br.com.bartzeit.tenants.domain.Barbershop;
import br.com.bartzeit.tenants.infrastructure.BarbershopRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BarbershopService {
    private final BarbershopRepository barbershopRepository;
    private final TenantResolver tenantResolver;

    public BarbershopService(BarbershopRepository barbershopRepository, TenantResolver tenantResolver) {
        this.barbershopRepository = barbershopRepository;
        this.tenantResolver = tenantResolver;
    }

    public BarbershopResponse createBarbershop(CreateBarbershopRequest request) {
        String slug = tenantResolver.resolve(request.slug());
        if (barbershopRepository.existsBySlug(slug)) {
            throw new ConflictException("A barbershop with slug '%s' already exists.".formatted(slug));
        }

        Barbershop barbershop = new Barbershop(
                UUID.randomUUID(),
                request.name(),
                slug,
                request.city(),
                request.address(),
                request.phone(),
                true
        );

        Barbershop saved = barbershopRepository.save(barbershop);
        return toResponse(saved);
    }

    public List<BarbershopResponse> listBarbershops(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = safePage * safeSize;
        return barbershopRepository.findAll(offset, safeSize).stream()
                .filter(Barbershop::isActive)
                .map(this::toResponse)
                .toList();
    }

    public BarbershopResponse getBySlug(String slug) {
        String tenantSlug = tenantResolver.resolve(slug);
        return barbershopRepository.findBySlug(tenantSlug)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(tenantSlug)));
    }

    public Barbershop getRequiredEntity(String tenantSlug) {
        String normalized = tenantResolver.resolve(tenantSlug);
        return barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));
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

    public record CreateBarbershopRequest(
            String name,
            String slug,
            String city,
            String address,
            String phone
    ) {
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
