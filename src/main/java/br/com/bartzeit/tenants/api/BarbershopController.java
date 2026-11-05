package br.com.bartzeit.tenants.api;

import br.com.bartzeit.tenants.application.BarbershopService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BarbershopController {
    private final BarbershopService barbershopService;

    public BarbershopController(BarbershopService barbershopService) {
        this.barbershopService = barbershopService;
    }

    @PostMapping("/v1/barbershops")
    public ResponseEntity<BarbershopService.BarbershopResponse> createBarbershop(@Valid @RequestBody CreateBarbershopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(barbershopService.createBarbershop(request.toServiceRequest()));
    }

    @GetMapping({"/v1/barbershops", "/public/barbershops"})
    public List<BarbershopService.BarbershopResponse> listBarbershops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return barbershopService.listBarbershops(page, size);
    }

    @GetMapping({"/v1/barbershops/{tenantSlug}", "/public/barbershops/{tenantSlug}"})
    public BarbershopService.BarbershopResponse getBySlug(@PathVariable String tenantSlug) {
        return barbershopService.getBySlug(tenantSlug);
    }

    public record CreateBarbershopRequest(
            @NotBlank(message = "Barbershop name is required.")
            String name,
            @NotBlank(message = "Slug is required.")
            @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase and may contain only letters, numbers and hyphens.")
            String slug,
            @NotBlank(message = "City is required.")
            String city,
            @NotBlank(message = "Address is required.")
            String address,
            String phone
    ) {
        public BarbershopService.CreateBarbershopRequest toServiceRequest() {
            return new BarbershopService.CreateBarbershopRequest(name, slug, city, address, phone);
        }
    }
}
