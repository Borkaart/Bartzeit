package br.com.bartzeit.services.api;

import br.com.bartzeit.services.application.ServiceCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ServiceController {
    private final ServiceCatalogService serviceCatalogService;

    public ServiceController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @PostMapping("/v1/barbershops/{tenantSlug}/services")
    public ResponseEntity<ServiceCatalogService.ServiceResponse> createService(
            @PathVariable String tenantSlug,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceCatalogService.createService(tenantSlug, request.toServiceRequest()));
    }

    @GetMapping({"/v1/barbershops/{tenantSlug}/services", "/public/barbershops/{tenantSlug}/services"})
    public List<ServiceCatalogService.ServiceResponse> listServices(@PathVariable String tenantSlug) {
        return serviceCatalogService.listServices(tenantSlug);
    }

    @GetMapping({"/v1/barbershops/{tenantSlug}/services/{serviceId}", "/public/barbershops/{tenantSlug}/services/{serviceId}"})
    public ServiceCatalogService.ServiceResponse getService(@PathVariable String tenantSlug, @PathVariable UUID serviceId) {
        return serviceCatalogService.getService(tenantSlug, serviceId);
    }

    public record CreateServiceRequest(
            @NotBlank(message = "Service name is required.")
            String name,
            String description,
            @Min(value = 1, message = "Duration must be at least 1 minute.")
            int durationMinutes,
            @NotNull(message = "Price is required.")
            @DecimalMin(value = "0.01", inclusive = true, message = "Price must be greater than zero.")
            BigDecimal price
    ) {
        public ServiceCatalogService.CreateServiceRequest toServiceRequest() {
            return new ServiceCatalogService.CreateServiceRequest(name, description, durationMinutes, price);
        }
    }
}
