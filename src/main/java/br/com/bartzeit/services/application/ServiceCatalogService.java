package br.com.bartzeit.services.application;

import br.com.bartzeit.services.domain.ServiceOffering;
import br.com.bartzeit.services.infrastructure.ServiceRepository;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceCatalogService {
    private final ServiceRepository serviceRepository;

    public ServiceCatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public ServiceResponse createService(TenantContext tenant, CreateServiceRequest request) {
        validate(request);
        ServiceOffering service = new ServiceOffering(
                UUID.randomUUID(),
                tenant.barbershopId(),
                request.name(),
                request.description(),
                request.durationMinutes(),
                request.price(),
                true
        );
        ServiceOffering saved = serviceRepository.save(service);
        return toResponse(saved);
    }

    public List<ServiceResponse> listServices(TenantContext tenant) {
        return serviceRepository.findAllByBarbershopId(tenant.barbershopId()).stream()
                .filter(ServiceOffering::isActive)
                .map(this::toResponse)
                .toList();
    }

    public ServiceResponse getService(TenantContext tenant, UUID serviceId) {
        ServiceOffering service = serviceRepository.findByIdAndBarbershopId(serviceId, tenant.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Service was not found in this tenant."));
        return toResponse(service);
    }

    public ServiceOffering requireServiceInTenant(TenantContext tenant, UUID serviceId) {
        return serviceRepository.findByIdAndBarbershopId(serviceId, tenant.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Service was not found in this tenant."));
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
