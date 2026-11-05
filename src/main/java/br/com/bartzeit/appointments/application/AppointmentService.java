package br.com.bartzeit.appointments.application;

import br.com.bartzeit.appointments.domain.Appointment;
import br.com.bartzeit.appointments.domain.AppointmentStatus;
import br.com.bartzeit.appointments.infrastructure.AppointmentRepository;
import br.com.bartzeit.barbers.application.BarberService;
import br.com.bartzeit.barbers.domain.Barber;
import br.com.bartzeit.barbers.infrastructure.BarberRepository;
import br.com.bartzeit.customers.domain.Customer;
import br.com.bartzeit.customers.infrastructure.CustomerRepository;
import br.com.bartzeit.services.application.ServiceCatalogService;
import br.com.bartzeit.services.domain.ServiceOffering;
import br.com.bartzeit.shared.error.ConflictException;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantResolver;
import br.com.bartzeit.tenants.domain.Barbershop;
import br.com.bartzeit.tenants.infrastructure.BarbershopRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final BarbershopRepository barbershopRepository;
    private final BarberRepository barberRepository;
    private final CustomerRepository customerRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final BarberService barberService;
    private final TenantResolver tenantResolver;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              BarbershopRepository barbershopRepository,
                              BarberRepository barberRepository,
                              CustomerRepository customerRepository,
                              ServiceCatalogService serviceCatalogService,
                              BarberService barberService,
                              TenantResolver tenantResolver) {
        this.appointmentRepository = appointmentRepository;
        this.barbershopRepository = barbershopRepository;
        this.barberRepository = barberRepository;
        this.customerRepository = customerRepository;
        this.serviceCatalogService = serviceCatalogService;
        this.barberService = barberService;
        this.tenantResolver = tenantResolver;
    }

    public AppointmentResponse createAppointment(String tenantSlug, CreateAppointmentRequest request) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        ServiceOffering service = serviceCatalogService.requireServiceInTenant(normalized, request.serviceId());
        Customer customer = resolveCustomer(normalized, request.customerId(), request.customerName());

        LocalDateTime startsAt = request.startsAt();
        if (startsAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointments cannot be scheduled in the past.");
        }

        Barber barber = resolveAvailableBarber(barbershop, request.barberId(), startsAt, service.getDurationMinutes());

        Appointment appointment = new Appointment(
                UUID.randomUUID(),
                barbershop.getId(),
                service.getId(),
                barber.getId(),
                customer.getId(),
                customer.getName(),
                startsAt,
                service.getDurationMinutes(),
                AppointmentStatus.CONFIRMED,
                request.notes()
        );

        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    public List<AppointmentResponse> listAppointments(String tenantSlug, LocalDateTime from, LocalDateTime to) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        LocalDateTime start = from == null ? LocalDateTime.now().minusDays(7) : from;
        LocalDateTime end = to == null ? LocalDateTime.now().plusDays(30) : to;
        return appointmentRepository.findBetween(barbershop.getId(), start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    public AppointmentResponse getAppointment(String tenantSlug, UUID appointmentId) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment '%s' was not found.".formatted(appointmentId)));
        if (!appointment.getBarbershopId().equals(barbershop.getId())) {
            throw new ResourceNotFoundException("Appointment '%s' was not found in tenant '%s'.".formatted(appointmentId, normalized));
        }
        return toResponse(appointment);
    }

    public List<AppointmentResponse> listAppointmentsForBarber(String tenantSlug, UUID barberId, LocalDateTime from, LocalDateTime to) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));
        Barber barber = barberService.requireBarberInTenant(normalized, barberId);
        if (!barber.getBarbershopId().equals(barbershop.getId())) {
            throw new ResourceNotFoundException("Barber '%s' was not found in tenant '%s'.".formatted(barberId, normalized));
        }
        LocalDateTime start = from == null ? LocalDateTime.now().minusDays(7) : from;
        LocalDateTime end = to == null ? LocalDateTime.now().plusDays(30) : to;
        return appointmentRepository.findByBarberBetween(barber.getId(), start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BarberAvailabilityResponse> getAvailability(String tenantSlug, LocalDate date, UUID serviceId) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        ServiceOffering service = serviceCatalogService.requireServiceInTenant(normalized, serviceId);
        List<Barber> barbers = barberRepository.findAllByBarbershopId(barbershop.getId()).stream()
                .filter(Barber::isActive)
                .toList();

        List<BarberAvailabilityResponse> responses = new ArrayList<>();
        for (Barber barber : barbers) {
            List<LocalDateTime> availableSlots = new ArrayList<>();
            LocalDateTime startOfDay = date.atTime(LocalTime.of(9, 0));
            LocalDateTime endOfDay = date.atTime(LocalTime.of(20, 0));
            LocalDateTime current = startOfDay;
            while (!current.plusMinutes(service.getDurationMinutes()).isAfter(endOfDay)) {
                LocalDateTime slotEnd = current.plusMinutes(service.getDurationMinutes());
                boolean overlap = appointmentRepository.hasOverlap(barber.getId(), current, slotEnd);
                if (!overlap && current.isAfter(LocalDateTime.now())) {
                    availableSlots.add(current);
                }
                current = current.plusMinutes(30);
            }
            responses.add(new BarberAvailabilityResponse(barber.getId(), barber.getName(), !availableSlots.isEmpty(), availableSlots));
        }
        return responses;
    }

    public AppointmentResponse updateStatus(String tenantSlug, UUID appointmentId, AppointmentStatus status) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment '%s' was not found.".formatted(appointmentId)));
        if (!appointment.getBarbershopId().equals(barbershop.getId())) {
            throw new ResourceNotFoundException("Appointment '%s' was not found in tenant '%s'.".formatted(appointmentId, normalized));
        }
        if (!appointment.canTransitionTo(status)) {
            throw new ConflictException("The status transition from '%s' to '%s' is not allowed.".formatted(appointment.getStatus(), status));
        }
        appointment.setStatus(status);
        return toResponse(appointmentRepository.save(appointment));
    }

    private Customer resolveCustomer(String tenantSlug, UUID customerId, String customerName) {
        if (customerId != null) {
            String normalized = tenantResolver.resolve(tenantSlug);
            Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                    .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));
            return customerRepository.findByIdAndBarbershopId(customerId, barbershop.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer '%s' was not found in tenant '%s'.".formatted(customerId, normalized)));
        }
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        return new Customer(UUID.randomUUID(), null, customerName, null, null);
    }

    private Barber resolveAvailableBarber(Barbershop barbershop, UUID barberId, LocalDateTime startsAt, int durationMinutes) {
        if (barberId != null) {
            Barber barber = barberRepository.findByIdAndBarbershopId(barberId, barbershop.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barber '%s' was not found in tenant '%s'.".formatted(barberId, barbershop.getSlug())));
            if (!barber.isActive()) {
                throw new ConflictException("Barber '%s' is inactive.".formatted(barberId));
            }
            if (appointmentRepository.hasOverlap(barber.getId(), startsAt, startsAt.plusMinutes(durationMinutes))) {
                throw new ConflictException("The selected barber has a conflicting appointment at the requested time.");
            }
            return barber;
        }

        List<Barber> activeBarbers = barberRepository.findAllByBarbershopId(barbershop.getId()).stream()
                .filter(Barber::isActive)
                .toList();
        if (activeBarbers.isEmpty()) {
            throw new ConflictException("There are no active barbers available in this barbershop.");
        }

        for (Barber barber : activeBarbers) {
            if (!appointmentRepository.hasOverlap(barber.getId(), startsAt, startsAt.plusMinutes(durationMinutes))) {
                return barber;
            }
        }
        throw new ConflictException("No barber is available at the requested time.");
    }

    public AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getBarbershopId(),
                appointment.getServiceId(),
                appointment.getBarberId(),
                appointment.getCustomerId(),
                appointment.getCustomerName(),
                appointment.getStartsAt(),
                appointment.getDurationMinutes(),
                appointment.getStatus(),
                appointment.getNotes()
        );
    }

    public record CreateAppointmentRequest(
            UUID serviceId,
            UUID barberId,
            UUID customerId,
            String customerName,
            LocalDateTime startsAt,
            String notes
    ) {
    }

    public record AppointmentResponse(
            UUID id,
            UUID barbershopId,
            UUID serviceId,
            UUID barberId,
            UUID customerId,
            String customerName,
            LocalDateTime startsAt,
            int durationMinutes,
            AppointmentStatus status,
            String notes
    ) {
    }

    public record BarberAvailabilityResponse(
            UUID barberId,
            String barberName,
            boolean available,
            List<LocalDateTime> slots
    ) {
    }
}
