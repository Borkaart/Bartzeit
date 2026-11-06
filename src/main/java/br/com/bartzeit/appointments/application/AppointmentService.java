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
import br.com.bartzeit.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final BarberRepository barberRepository;
    private final CustomerRepository customerRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final BarberService barberService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              BarberRepository barberRepository,
                              CustomerRepository customerRepository,
                              ServiceCatalogService serviceCatalogService,
                              BarberService barberService) {
        this.appointmentRepository = appointmentRepository;
        this.barberRepository = barberRepository;
        this.customerRepository = customerRepository;
        this.serviceCatalogService = serviceCatalogService;
        this.barberService = barberService;
    }

    @Transactional
    public AppointmentResponse createAppointment(TenantContext tenant, CreateAppointmentRequest request) {
        if (request.serviceId() == null || request.startsAt() == null) {
            throw new IllegalArgumentException("Service and start time are required.");
        }
        if (request.customerId() == null && (request.customerName() == null || request.customerName().isBlank())) {
            throw new IllegalArgumentException("A customer ID or customer name is required.");
        }

        ServiceOffering service = serviceCatalogService.requireServiceInTenant(tenant, request.serviceId());
        if (!service.isActive()) {
            throw new ConflictException("The selected service is inactive.");
        }
        Customer customer = resolveCustomer(tenant, request.customerId(), request.customerName(), null);

        LocalDateTime startsAt = request.startsAt();
        if (startsAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointments cannot be scheduled in the past.");
        }
        Barber barber = lockAvailableBarber(tenant, request.barberId(), startsAt, service.getDurationMinutes(), null);

        Appointment appointment = new Appointment(
                UUID.randomUUID(),
                tenant.barbershopId(),
                service.getId(),
                barber.getId(),
                customer.getId(),
                customer.getName(),
                startsAt,
                service.getDurationMinutes(),
                AppointmentStatus.CONFIRMED,
                request.notes()
        );
        return toResponse(appointmentRepository.save(appointment));
    }

    public List<AppointmentResponse> listAppointments(
            TenantContext tenant, LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = from == null ? LocalDateTime.now().minusDays(7) : from;
        LocalDateTime end = to == null ? LocalDateTime.now().plusDays(30) : to;
        validateRange(start, end);
        return appointmentRepository.findBetween(tenant.barbershopId(), start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    public AppointmentResponse getAppointment(TenantContext tenant, UUID appointmentId) {
        return appointmentRepository.findByIdAndBarbershopId(appointmentId, tenant.barbershopId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found in this tenant."));
    }

    @Transactional
    public AppointmentResponse updateAppointment(
            TenantContext tenant, UUID appointmentId, CreateAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndBarbershopId(appointmentId, tenant.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found in this tenant."));
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Completed or cancelled appointments cannot be changed.");
        }
        if (request.serviceId() == null || request.startsAt() == null) {
            throw new IllegalArgumentException("Service and start time are required.");
        }
        ServiceOffering service = serviceCatalogService.requireServiceInTenant(tenant, request.serviceId());
        if (!service.isActive()) {
            throw new ConflictException("The selected service is inactive.");
        }
        Customer customer = resolveCustomer(tenant, request.customerId(), request.customerName(), appointment);
        if (request.startsAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointments cannot be scheduled in the past.");
        }
        Barber barber = lockAvailableBarber(
                tenant, request.barberId(), request.startsAt(), service.getDurationMinutes(), appointmentId);

        appointment.setServiceId(service.getId());
        appointment.setBarberId(barber.getId());
        appointment.setCustomerId(customer.getId());
        appointment.setCustomerName(customer.getName());
        appointment.setStartsAt(request.startsAt());
        appointment.setDurationMinutes(service.getDurationMinutes());
        appointment.setNotes(request.notes());
        return toResponse(appointmentRepository.save(appointment));
    }

    public List<AppointmentResponse> listAppointmentsForBarber(
            TenantContext tenant, UUID barberId, LocalDateTime from, LocalDateTime to) {
        barberService.requireBarberInTenant(tenant, barberId);
        LocalDateTime start = from == null ? LocalDateTime.now().minusDays(7) : from;
        LocalDateTime end = to == null ? LocalDateTime.now().plusDays(30) : to;
        validateRange(start, end);
        return appointmentRepository.findByBarberBetween(tenant.barbershopId(), barberId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BarberAvailabilityResponse> getAvailability(TenantContext tenant, LocalDate date, UUID serviceId) {
        ServiceOffering service = serviceCatalogService.requireServiceInTenant(tenant, serviceId);
        List<Barber> barbers = barberRepository.findAllByBarbershopId(tenant.barbershopId()).stream()
                .filter(Barber::isActive)
                .toList();

        List<BarberAvailabilityResponse> responses = new ArrayList<>();
        for (Barber barber : barbers) {
            List<LocalDateTime> availableSlots = new ArrayList<>();
            LocalDateTime endOfDay = date.atTime(20, 0);
            LocalDateTime current = date.atTime(LocalTime.of(9, 0));
            while (!current.plusMinutes(service.getDurationMinutes()).isAfter(endOfDay)) {
                LocalDateTime slotEnd = current.plusMinutes(service.getDurationMinutes());
                boolean overlap = appointmentRepository.hasOverlap(
                        tenant.barbershopId(), barber.getId(), current, slotEnd, null);
                if (!overlap && current.isAfter(LocalDateTime.now())) {
                    availableSlots.add(current);
                }
                current = current.plusMinutes(30);
            }
            responses.add(new BarberAvailabilityResponse(barber.getId(), barber.getName(),
                    !availableSlots.isEmpty(), availableSlots));
        }
        return responses;
    }

    @Transactional
    public AppointmentResponse updateStatus(TenantContext tenant, UUID appointmentId, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findByIdAndBarbershopId(appointmentId, tenant.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found in this tenant."));
        if (!appointment.canTransitionTo(status)) {
            throw new ConflictException("The requested appointment status transition is not allowed.");
        }
        appointment.setStatus(status);
        return toResponse(appointmentRepository.save(appointment));
    }

    private Customer resolveCustomer(
            TenantContext tenant, UUID customerId, String customerName, Appointment existingAppointment) {
        if (customerId != null) {
            return customerRepository.findByIdAndBarbershopId(customerId, tenant.barbershopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer was not found in this tenant."));
        }
        if ((customerName == null || customerName.isBlank()) && existingAppointment != null) {
            return customerRepository.findByIdAndBarbershopId(existingAppointment.getCustomerId(), tenant.barbershopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer was not found in this tenant."));
        }
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("A customer ID or customer name is required.");
        }
        Customer customer = new Customer(UUID.randomUUID(), tenant.barbershopId(), customerName.trim(), null, null);
        return customerRepository.save(customer);
    }

    private Barber lockAvailableBarber(TenantContext tenant, UUID barberId, LocalDateTime startsAt, int durationMinutes,
                                       UUID excludedAppointmentId) {
        LocalDateTime endsAt = startsAt.plusMinutes(durationMinutes);
        if (barberId != null) {
            Barber barber = barberRepository.lockByIdAndBarbershopId(barberId, tenant.barbershopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barber was not found in this tenant."));
            if (!barber.isActive()) {
                throw new ConflictException("The selected barber is inactive.");
            }
            if (appointmentRepository.hasOverlap(
                    tenant.barbershopId(), barber.getId(), startsAt, endsAt, excludedAppointmentId)) {
                throw new ConflictException("The selected barber has a conflicting appointment at the requested time.");
            }
            return barber;
        }

        for (Barber candidate : barberRepository.findAllByBarbershopId(tenant.barbershopId())) {
            if (!candidate.isActive()) {
                continue;
            }
            Barber barber = barberRepository.lockByIdAndBarbershopId(candidate.getId(), tenant.barbershopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barber was not found in this tenant."));
            if (barber.isActive()
                    && !appointmentRepository.hasOverlap(
                            tenant.barbershopId(), barber.getId(), startsAt, endsAt, excludedAppointmentId)) {
                return barber;
            }
        }
        throw new ConflictException("No barber is available at the requested time.");
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("The start of the requested range must be before its end.");
        }
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
