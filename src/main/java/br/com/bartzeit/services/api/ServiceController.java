package br.com.bartzeit.services.api;

import br.com.bartzeit.services.application.ServiceCatalogService;
import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ServiceController {
    private final ServiceCatalogService serviceCatalogService;
    private final TenantAccessService tenantAccessService;

    public ServiceController(ServiceCatalogService serviceCatalogService, TenantAccessService tenantAccessService) {
        this.serviceCatalogService = serviceCatalogService;
        this.tenantAccessService = tenantAccessService;
    }

    @PostMapping("/v1/barbershops/{tenantSlug}/services")
    public ResponseEntity<ServiceCatalogService.ServiceResponse> createService(
            @PathVariable String tenantSlug,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceCatalogService.createService(
                        tenantAccessService.requireTenant(tenantSlug, principal), request.toServiceRequest()));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/services")
    public List<ServiceCatalogService.ServiceResponse> listServices(
            @PathVariable String tenantSlug, @AuthenticationPrincipal TenantUserPrincipal principal) {
        return serviceCatalogService.listServices(tenantAccessService.requireTenant(tenantSlug, principal));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/services/{serviceId}")
    public ServiceCatalogService.ServiceResponse getService(
            @PathVariable String tenantSlug, @PathVariable UUID serviceId,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return serviceCatalogService.getService(tenantAccessService.requireTenant(tenantSlug, principal), serviceId);
    }

    public record CreateServiceRequest(
            @NotBlank(message = "Service name is required.")
            @Size(max = 120)
            String name,
            @Size(max = 500)
            String description,
            @Min(value = 1, message = "Duration must be at least 1 minute.")
            @jakarta.validation.constraints.Max(value = 480)
            int durationMinutes,
            @NotNull(message = "Price is required.")
            @DecimalMin(value = "0.01", inclusive = true, message = "Price must be greater than zero.")
            @Digits(integer = 8, fraction = 2)
            BigDecimal price
    ) {
        public ServiceCatalogService.CreateServiceRequest toServiceRequest() {
            return new ServiceCatalogService.CreateServiceRequest(name, description, durationMinutes, price);
        }
    }
}
