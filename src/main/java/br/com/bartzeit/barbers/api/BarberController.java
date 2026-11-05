package br.com.bartzeit.barbers.api;

import br.com.bartzeit.barbers.application.BarberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BarberController {
    private final BarberService barberService;

    public BarberController(BarberService barberService) {
        this.barberService = barberService;
    }

    @PostMapping("/v1/barbershops/{tenantSlug}/barbers")
    public ResponseEntity<BarberService.BarberResponse> createBarber(
            @PathVariable String tenantSlug,
            @Valid @RequestBody CreateBarberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(barberService.createBarber(tenantSlug, request.toServiceRequest()));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/barbers")
    public List<BarberService.BarberResponse> listBarbers(@PathVariable String tenantSlug) {
        return barberService.listBarbers(tenantSlug);
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/barbers/{barberId}")
    public BarberService.BarberResponse getBarber(@PathVariable String tenantSlug, @PathVariable UUID barberId) {
        return barberService.getBarber(tenantSlug, barberId);
    }

    @PatchMapping("/v1/barbershops/{tenantSlug}/barbers/{barberId}/status")
    public BarberService.BarberResponse updateStatus(@PathVariable String tenantSlug, @PathVariable UUID barberId,
                                                  @RequestBody UpdateStatusRequest request) {
        return barberService.updateStatus(tenantSlug, barberId, request.active());
    }

    public record CreateBarberRequest(
            @NotBlank(message = "Barber name is required.")
            String name,
            String specialty
    ) {
        public BarberService.CreateBarberRequest toServiceRequest() {
            return new BarberService.CreateBarberRequest(name, specialty);
        }
    }

    public record UpdateStatusRequest(Boolean active) {
    }
}
