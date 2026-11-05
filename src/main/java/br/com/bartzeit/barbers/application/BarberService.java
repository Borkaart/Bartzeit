package br.com.bartzeit.barbers.application;

import br.com.bartzeit.barbers.domain.Barber;
import br.com.bartzeit.barbers.infrastructure.BarberRepository;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantResolver;
import br.com.bartzeit.tenants.domain.Barbershop;
import br.com.bartzeit.tenants.infrastructure.BarbershopRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BarberService {
    private final BarberRepository barberRepository;
    private final BarbershopRepository barbershopRepository;
    private final TenantResolver tenantResolver;

    public BarberService(BarberRepository barberRepository, BarbershopRepository barbershopRepository, TenantResolver tenantResolver) {
        this.barberRepository = barberRepository;
        this.barbershopRepository = barbershopRepository;
        this.tenantResolver = tenantResolver;
    }

    public BarberResponse createBarber(String tenantSlug, CreateBarberRequest request) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        Barber barber = new Barber(
                UUID.randomUUID(),
                barbershop.getId(),
                request.name(),
                request.specialty(),
                true
        );

        return toResponse(barberRepository.save(barber));
    }

    public List<BarberResponse> listBarbers(String tenantSlug) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        return barberRepository.findAllByBarbershopId(barbershop.getId()).stream()
                .filter(Barber::isActive)
                .map(this::toResponse)
                .toList();
    }

    public BarberResponse getBarber(String tenantSlug, UUID barberId) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        Barber barber = barberRepository.findByIdAndBarbershopId(barberId, barbershop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber '%s' was not found in tenant '%s'.".formatted(barberId, normalized)));
        return toResponse(barber);
    }

    public BarberResponse updateStatus(String tenantSlug, UUID barberId, boolean active) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        Barber barber = barberRepository.findByIdAndBarbershopId(barberId, barbershop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber '%s' was not found in tenant '%s'.".formatted(barberId, normalized)));
        barber.setActive(active);
        return toResponse(barberRepository.save(barber));
    }

    public Barber requireBarberInTenant(String tenantSlug, UUID barberId) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));
        return barberRepository.findByIdAndBarbershopId(barberId, barbershop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber '%s' was not found in tenant '%s'.".formatted(barberId, normalized)));
    }

    public BarberResponse toResponse(Barber barber) {
        return new BarberResponse(barber.getId(), barber.getBarbershopId(), barber.getName(), barber.getSpecialty(), barber.isActive());
    }

    public record CreateBarberRequest(String name, String specialty) {
    }

    public record BarberResponse(UUID id, UUID barbershopId, String name, String specialty, boolean active) {
    }
}
