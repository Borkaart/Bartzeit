package br.com.bartzeit.customers.infrastructure;

import br.com.bartzeit.customers.domain.Customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findByIdAndBarbershopId(UUID id, UUID barbershopId);
    List<Customer> findAllByBarbershopId(UUID barbershopId);
}
