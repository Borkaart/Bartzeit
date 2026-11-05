package br.com.bartzeit.services.application;

import br.com.bartzeit.services.domain.ServiceOffering;
import br.com.bartzeit.services.infrastructure.ServiceRepository;
import br.com.bartzeit.shared.error.ConflictException;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantResolver;
import br.com.bartzeit.tenants.domain.Barbershop;
import br.com.bartzeit.tenants.infrastructure.BarbershopRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceCatalogService {
    private final ServiceRepository serviceRepository;
    private final BarbershopRepository barbershopRepository;
    private final TenantResolver tenantResolver;

    public ServiceCatalogService(ServiceRepository serviceRepository, BarbershopRepository barbershopRepository, TenantResolver tenantResolver) {
        this.serviceRepository = serviceRepository;
        this.barbershopRepository = barbershopRepository;
        this.tenantResolver = tenantResolver;
    }

    public ServiceResponse createService(String tenantSlug, CreateServiceRequest request) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        validate(request);
        ServiceOffering service = new ServiceOffering(
                UUID.randomUUID(),
                barbershop.getId(),
                request.name(),
                request.description(),
                request.durationMinutes(),
                request.price(),
                true
        );
        ServiceOffering saved = serviceRepository.save(service);
        return toResponse(saved);
    }

    public List<ServiceResponse> listServices(String tenantSlug) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        return serviceRepository.findAllByBarbershopId(barbershop.getId()).stream()
                .filter(ServiceOffering::isActive)
                .map(this::toResponse)
                .toList();
    }

    public ServiceResponse getService(String tenantSlug, UUID serviceId) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        ServiceOffering service = serviceRepository.findByIdAndBarbershopId(serviceId, barbershop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service '%s' was not found in tenant '%s'.".formatted(serviceId, normalized)));
        return toResponse(service);
    }

    public ServiceOffering requireServiceInTenant(String tenantSlug, UUID serviceId) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));
        return serviceRepository.findByIdAndBarbershopId(serviceId, barbershop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service '%s' was not found in tenant '%s'.".formatted(serviceId, normalized)));
    }

    private void validate(CreateServiceRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Service name is required.");
        }
        if (request.durationMinutes() <= 0 || request.durationMinutes() > 480) {
            throw new IllegalArgumentException("Duration must be between 1 and 480 minutes.");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be a positive value.");
        }
    }

    public ServiceResponse toResponse(ServiceOffering service) {
        return new ServiceResponse(
                service.getId(),
                service.getBarbershopId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPrice(),
                service.isActive()
        );
    }

    public record CreateServiceRequest(
            String name,
            String description,
            int durationMinutes,
            BigDecimal price
    ) {
    }

    public record ServiceResponse(
            UUID id,
            UUID barbershopId,
            String name,
            String description,
            int durationMinutes,
            BigDecimal price,
            boolean active
    ) {
    }
}
