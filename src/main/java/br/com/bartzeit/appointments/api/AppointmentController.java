package br.com.bartzeit.appointments.api;

import br.com.bartzeit.appointments.application.AppointmentService;
import br.com.bartzeit.appointments.domain.AppointmentStatus;
import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final TenantAccessService tenantAccessService;

    public AppointmentController(AppointmentService appointmentService, TenantAccessService tenantAccessService) {
        this.appointmentService = appointmentService;
        this.tenantAccessService = tenantAccessService;
    }

    @PostMapping("/v1/barbershops/{tenantSlug}/appointments")
    public ResponseEntity<AppointmentService.AppointmentResponse> createAppointment(
            @PathVariable String tenantSlug,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(
                        tenantAccessService.requireTenant(tenantSlug, principal), request.toServiceRequest()));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/appointments")
    public List<AppointmentService.AppointmentResponse> listAppointments(
            @PathVariable String tenantSlug,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return appointmentService.listAppointments(tenantAccessService.requireTenant(tenantSlug, principal), from, to);
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/barbers/{barberId}/appointments")
    public List<AppointmentService.AppointmentResponse> listAppointmentsForBarber(
            @PathVariable String tenantSlug,
            @PathVariable UUID barberId,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return appointmentService.listAppointmentsForBarber(
                tenantAccessService.requireTenant(tenantSlug, principal), barberId, from, to);
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/availability")
    public List<AppointmentService.BarberAvailabilityResponse> getAvailability(
            @PathVariable String tenantSlug,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID serviceId
    ) {
        return appointmentService.getAvailability(tenantAccessService.requireTenant(tenantSlug, principal), date, serviceId);
    }

    @PatchMapping("/v1/barbershops/{tenantSlug}/appointments/{appointmentId}/status")
    public AppointmentService.AppointmentResponse updateStatus(
            @PathVariable String tenantSlug,
            @PathVariable UUID appointmentId,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return appointmentService.updateStatus(
                tenantAccessService.requireTenant(tenantSlug, principal), appointmentId, request.status());
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/appointments/{appointmentId}")
    public AppointmentService.AppointmentResponse getAppointment(
            @PathVariable String tenantSlug, @PathVariable UUID appointmentId,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return appointmentService.getAppointment(tenantAccessService.requireTenant(tenantSlug, principal), appointmentId);
    }

    @PutMapping("/v1/barbershops/{tenantSlug}/appointments/{appointmentId}")
    public AppointmentService.AppointmentResponse updateAppointment(
            @PathVariable String tenantSlug,
            @PathVariable UUID appointmentId,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.updateAppointment(
                tenantAccessService.requireTenant(tenantSlug, principal),
                appointmentId,
                request.toServiceRequest());
    }

    public record CreateAppointmentRequest(
            @NotNull UUID serviceId,
            UUID barberId,
            UUID customerId,
            @Size(max = 120)
            String customerName,
            @NotNull LocalDateTime startsAt,
            @Size(max = 1000)
            String notes
    ) {
        public AppointmentService.CreateAppointmentRequest toServiceRequest() {
            return new AppointmentService.CreateAppointmentRequest(serviceId, barberId, customerId, customerName, startsAt, notes);
        }
    }

    public record UpdateStatusRequest(@NotNull AppointmentStatus status) {
    }
}
