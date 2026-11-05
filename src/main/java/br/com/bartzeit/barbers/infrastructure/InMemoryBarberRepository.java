package br.com.bartzeit.barbers.infrastructure;

import br.com.bartzeit.barbers.domain.Barber;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBarberRepository implements BarberRepository {
    private final Map<UUID, Barber> stores = new ConcurrentHashMap<>();

    @Override
    public Barber save(Barber barber) {
        stores.put(barber.getId(), barber);
        return barber;
    }

    @Override
    public Optional<Barber> findById(UUID id) {
        return Optional.ofNullable(stores.get(id));
    }

    @Override
    public Optional<Barber> findByIdAndBarbershopId(UUID id, UUID barbershopId) {
        return stores.values().stream()
                .filter(barber -> barber.getId().equals(id))
                .filter(barber -> barber.getBarbershopId().equals(barbershopId))
                .findFirst();
    }

    @Override
    public List<Barber> findAllByBarbershopId(UUID barbershopId) {
        return stores.values().stream()
                .filter(barber -> barber.getBarbershopId().equals(barbershopId))
                .sorted(Comparator.comparing(Barber::getName))
                .toList();
    }
}
