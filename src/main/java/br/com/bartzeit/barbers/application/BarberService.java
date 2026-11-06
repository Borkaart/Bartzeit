package br.com.bartzeit.barbers.application;

import br.com.bartzeit.barbers.domain.Barber;
import br.com.bartzeit.barbers.infrastructure.BarberRepository;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BarberService {
    private final BarberRepository barberRepository;

    public BarberService(BarberRepository barberRepository) {
        this.barberRepository = barberRepository;
    }

    public BarberResponse createBarber(TenantContext tenant, CreateBarberRequest request) {
        Barber barber = new Barber(
                UUID.randomUUID(),
                tenant.barbershopId(),
                request.name(),
                request.specialty(),
                true
        );

        return toResponse(barberRepository.save(barber));
    }

    public List<BarberResponse> listBarbers(TenantContext tenant) {
        return barberRepository.findAllByBarbershopId(tenant.barbershopId()).stream()
                .filter(Barber::isActive)
                .map(this::toResponse)
                .toList();
    }

    public BarberResponse getBarber(TenantContext tenant, UUID barberId) {
        Barber barber = barberRepository.findByIdAndBarbershopId(barberId, tenant.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber was not found in this tenant."));
        return toResponse(barber);
    }

    public BarberResponse updateStatus(TenantContext tenant, UUID barberId, boolean active) {
        Barber barber = barberRepository.findByIdAndBarbershopId(barberId, tenant.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber was not found in this tenant."));
        barber.setActive(active);
        return toResponse(barberRepository.save(barber));
    }

    public Barber requireBarberInTenant(TenantContext tenant, UUID barberId) {
        return barberRepository.findByIdAndBarbershopId(barberId, tenant.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber was not found in this tenant."));
    }

    public BarberResponse toResponse(Barber barber) {
        return new BarberResponse(barber.getId(), barber.getBarbershopId(), barber.getName(), barber.getSpecialty(), barber.isActive());
    }

    public record CreateBarberRequest(String name, String specialty) {
    }

    public record BarberResponse(UUID id, UUID barbershopId, String name, String specialty, boolean active) {
    }
}
