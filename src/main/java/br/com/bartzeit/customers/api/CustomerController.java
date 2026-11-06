package br.com.bartzeit.customers.api;

import br.com.bartzeit.customers.application.CustomerService;
import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CustomerController {
    private final CustomerService customerService;
    private final TenantAccessService tenantAccessService;

    public CustomerController(CustomerService customerService, TenantAccessService tenantAccessService) {
        this.customerService = customerService;
        this.tenantAccessService = tenantAccessService;
    }

    @PostMapping("/v1/barbershops/{tenantSlug}/customers")
    public ResponseEntity<CustomerService.CustomerResponse> createCustomer(
            @PathVariable String tenantSlug,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(
                tenantAccessService.requireTenant(tenantSlug, principal), request.toServiceRequest()));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/customers")
    public List<CustomerService.CustomerResponse> listCustomers(
            @PathVariable String tenantSlug, @AuthenticationPrincipal TenantUserPrincipal principal) {
        return customerService.listCustomers(tenantAccessService.requireTenant(tenantSlug, principal));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/customers/{customerId}")
    public CustomerService.CustomerResponse getCustomer(
            @PathVariable String tenantSlug, @PathVariable UUID customerId,
            @AuthenticationPrincipal TenantUserPrincipal principal) {
        return customerService.getCustomer(tenantAccessService.requireTenant(tenantSlug, principal), customerId);
    }

    public record CreateCustomerRequest(
            @NotBlank(message = "Customer name is required.")
            @Size(max = 120)
            String name,
            @Email(message = "Customer email must be valid.")
            @Size(max = 254)
            String email,
            @Size(max = 40)
            String phone
    ) {
        public CustomerService.CreateCustomerRequest toServiceRequest() {
            return new CustomerService.CreateCustomerRequest(name, email, phone);
        }
    }
}
