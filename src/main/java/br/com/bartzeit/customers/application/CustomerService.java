package br.com.bartzeit.customers.application;

import br.com.bartzeit.customers.domain.Customer;
import br.com.bartzeit.customers.infrastructure.CustomerRepository;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantResolver;
import br.com.bartzeit.tenants.domain.Barbershop;
import br.com.bartzeit.tenants.infrastructure.BarbershopRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final BarbershopRepository barbershopRepository;
    private final TenantResolver tenantResolver;

    public CustomerService(CustomerRepository customerRepository, BarbershopRepository barbershopRepository, TenantResolver tenantResolver) {
        this.customerRepository = customerRepository;
        this.barbershopRepository = barbershopRepository;
        this.tenantResolver = tenantResolver;
    }

    public CustomerResponse createCustomer(String tenantSlug, CreateCustomerRequest request) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        Customer customer = new Customer(UUID.randomUUID(), barbershop.getId(), request.name(), request.email(), request.phone());
        return toResponse(customerRepository.save(customer));
    }

    public List<CustomerResponse> listCustomers(String tenantSlug) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));
        return customerRepository.findAllByBarbershopId(barbershop.getId()).stream().map(this::toResponse).toList();
    }

    public CustomerResponse getCustomer(String tenantSlug, UUID customerId) {
        String normalized = tenantResolver.resolve(tenantSlug);
        Barbershop barbershop = barbershopRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Barbershop '%s' was not found.".formatted(normalized)));

        Customer customer = customerRepository.findByIdAndBarbershopId(customerId, barbershop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer '%s' was not found in tenant '%s'.".formatted(customerId, normalized)));
        return toResponse(customer);
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getBarbershopId(), customer.getName(), customer.getEmail(), customer.getPhone());
    }

    public record CreateCustomerRequest(String name, String email, String phone) {
    }

    public record CustomerResponse(UUID id, UUID barbershopId, String name, String email, String phone) {
    }
}
