package br.com.bartzeit.barbers.api;

import br.com.bartzeit.barbers.application.BarberService;
import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BarberController {
    private final BarberService barberService;
    private final TenantAccessService tenantAccessService;

    public BarberController(BarberService barberService, TenantAccessService tenantAccessService) {
        this.barberService = barberService;
        this.tenantAccessService = tenantAccessService;
    }

    @PostMapping("/v1/barbershops/{tenantSlug}/barbers")
    public ResponseEntity<BarberService.BarberResponse> createBarber(
            @PathVariable String tenantSlug,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @Valid @RequestBody CreateBarberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(barberService.createBarber(
                tenantAccessService.requireTenant(tenantSlug, principal), request.toServiceRequest()));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/barbers")
    public List<BarberService.BarberResponse> listBarbers(
            @PathVariable String tenantSlug, @AuthenticationPrincipal TenantUserPrincipal principal) {
        return barberService.listBarbers(tenantAccessService.requireTenant(tenantSlug, principal));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/barbers/{barberId}")
    public BarberService.BarberResponse getBarber(
            @PathVariable String tenantSlug, @PathVariable UUID barberId,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return barberService.getBarber(tenantAccessService.requireTenant(tenantSlug, principal), barberId);
    }

    @PatchMapping("/v1/barbershops/{tenantSlug}/barbers/{barberId}/status")
    public BarberService.BarberResponse updateStatus(@PathVariable String tenantSlug, @PathVariable UUID barberId,
                                                  @AuthenticationPrincipal TenantUserPrincipal principal,
                                                  @Valid @RequestBody UpdateStatusRequest request) {
        return barberService.updateStatus(
                tenantAccessService.requireTenant(tenantSlug, principal), barberId, request.active());
    }

    public record CreateBarberRequest(
            @NotBlank(message = "Barber name is required.")
            @Size(max = 120)
            String name,
            @Size(max = 120)
            String specialty
    ) {
        public BarberService.CreateBarberRequest toServiceRequest() {
            return new BarberService.CreateBarberRequest(name, specialty);
        }
    }

    public record UpdateStatusRequest(@NotNull Boolean active) {
    }
}
