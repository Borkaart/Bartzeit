package br.com.bartzeit.services.infrastructure;

import br.com.bartzeit.services.domain.ServiceOffering;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryServiceRepository implements ServiceRepository {
    private final Map<UUID, ServiceOffering> stores = new ConcurrentHashMap<>();

    @Override
    public ServiceOffering save(ServiceOffering offering) {
        stores.put(offering.getId(), offering);
        return offering;
    }

    @Override
    public Optional<ServiceOffering> findById(UUID id) {
        return Optional.ofNullable(stores.get(id));
    }

    @Override
    public Optional<ServiceOffering> findByIdAndBarbershopId(UUID id, UUID barbershopId) {
        return stores.values().stream()
                .filter(service -> service.getId().equals(id))
                .filter(service -> service.getBarbershopId().equals(barbershopId))
                .findFirst();
    }

    @Override
    public List<ServiceOffering> findAllByBarbershopId(UUID barbershopId) {
        return stores.values().stream()
                .filter(service -> service.getBarbershopId().equals(barbershopId))
                .sorted(Comparator.comparing(ServiceOffering::getName))
                .toList();
    }
}
