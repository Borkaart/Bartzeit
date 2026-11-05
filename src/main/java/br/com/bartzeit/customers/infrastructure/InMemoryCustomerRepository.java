package br.com.bartzeit.customers.infrastructure;

import br.com.bartzeit.customers.domain.Customer;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCustomerRepository implements CustomerRepository {
    private final Map<UUID, Customer> stores = new ConcurrentHashMap<>();

    @Override
    public Customer save(Customer customer) {
        stores.put(customer.getId(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return Optional.ofNullable(stores.get(id));
    }

    @Override
    public Optional<Customer> findByIdAndBarbershopId(UUID id, UUID barbershopId) {
        return stores.values().stream()
                .filter(customer -> customer.getId().equals(id))
                .filter(customer -> customer.getBarbershopId().equals(barbershopId))
                .findFirst();
    }

    @Override
    public List<Customer> findAllByBarbershopId(UUID barbershopId) {
        return stores.values().stream()
                .filter(customer -> customer.getBarbershopId().equals(barbershopId))
                .sorted(Comparator.comparing(Customer::getName))
                .toList();
    }
}
