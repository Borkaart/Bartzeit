package br.com.bartzeit.customers.application;

import br.com.bartzeit.customers.domain.Customer;
import br.com.bartzeit.customers.infrastructure.CustomerRepository;
import br.com.bartzeit.shared.error.ResourceNotFoundException;
import br.com.bartzeit.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(TenantContext tenant, CreateCustomerRequest request) {
        Customer customer = new Customer(UUID.randomUUID(), tenant.barbershopId(), request.name(), request.email(), request.phone());
        return toResponse(customerRepository.save(customer));
    }

    public List<CustomerResponse> listCustomers(TenantContext tenant) {
        return customerRepository.findAllByBarbershopId(tenant.barbershopId()).stream().map(this::toResponse).toList();
    }

    public CustomerResponse getCustomer(TenantContext tenant, UUID customerId) {
        Customer customer = customerRepository.findByIdAndBarbershopId(customerId, tenant.barbershopId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer was not found in this tenant."));
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
