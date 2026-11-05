package br.com.bartzeit.appointments.api;

import br.com.bartzeit.appointments.application.AppointmentService;
import br.com.bartzeit.appointments.domain.AppointmentStatus;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/v1/barbershops/{tenantSlug}/appointments")
    public ResponseEntity<AppointmentService.AppointmentResponse> createAppointment(
            @PathVariable String tenantSlug,
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(tenantSlug, request.toServiceRequest()));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/appointments")
    public List<AppointmentService.AppointmentResponse> listAppointments(
            @PathVariable String tenantSlug,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return appointmentService.listAppointments(tenantSlug, from, to);
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/barbers/{barberId}/appointments")
    public List<AppointmentService.AppointmentResponse> listAppointmentsForBarber(
            @PathVariable String tenantSlug,
            @PathVariable UUID barberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return appointmentService.listAppointmentsForBarber(tenantSlug, barberId, from, to);
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/availability")
    public List<AppointmentService.BarberAvailabilityResponse> getAvailability(
            @PathVariable String tenantSlug,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID serviceId
    ) {
        return appointmentService.getAvailability(tenantSlug, date, serviceId);
    }

    @PatchMapping("/v1/barbershops/{tenantSlug}/appointments/{appointmentId}/status")
    public AppointmentService.AppointmentResponse updateStatus(
            @PathVariable String tenantSlug,
            @PathVariable UUID appointmentId,
            @RequestBody UpdateStatusRequest request
    ) {
        return appointmentService.updateStatus(tenantSlug, appointmentId, request.status());
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/appointments/{appointmentId}")
    public AppointmentService.AppointmentResponse getAppointment(@PathVariable String tenantSlug, @PathVariable UUID appointmentId) {
        return appointmentService.getAppointment(tenantSlug, appointmentId);
    }

    public record CreateAppointmentRequest(
            UUID serviceId,
            UUID barberId,
            UUID customerId,
            String customerName,
            LocalDateTime startsAt,
            String notes
    ) {
        public AppointmentService.CreateAppointmentRequest toServiceRequest() {
            return new AppointmentService.CreateAppointmentRequest(serviceId, barberId, customerId, customerName, startsAt, notes);
        }
    }

    public record UpdateStatusRequest(AppointmentStatus status) {
    }
}
