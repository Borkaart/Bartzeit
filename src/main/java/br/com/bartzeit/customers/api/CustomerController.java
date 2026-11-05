package br.com.bartzeit.customers.api;

import br.com.bartzeit.customers.application.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/v1/barbershops/{tenantSlug}/customers")
    public ResponseEntity<CustomerService.CustomerResponse> createCustomer(
            @PathVariable String tenantSlug,
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(tenantSlug, request.toServiceRequest()));
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/customers")
    public List<CustomerService.CustomerResponse> listCustomers(@PathVariable String tenantSlug) {
        return customerService.listCustomers(tenantSlug);
    }

    @GetMapping("/v1/barbershops/{tenantSlug}/customers/{customerId}")
    public CustomerService.CustomerResponse getCustomer(@PathVariable String tenantSlug, @PathVariable UUID customerId) {
        return customerService.getCustomer(tenantSlug, customerId);
    }

    public record CreateCustomerRequest(
            @NotBlank(message = "Customer name is required.")
            String name,
            @Email(message = "Customer email must be valid.")
            String email,
            String phone
    ) {
        public CustomerService.CreateCustomerRequest toServiceRequest() {
            return new CustomerService.CreateCustomerRequest(name, email, phone);
        }
    }
}
