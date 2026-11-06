package br.com.bartzeit.barbers.infrastructure;

import br.com.bartzeit.barbers.domain.Barber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BarberRepository {
    Barber save(Barber barber);
    Optional<Barber> findByIdAndBarbershopId(UUID id, UUID barbershopId);
    Optional<Barber> lockByIdAndBarbershopId(UUID id, UUID barbershopId);
    List<Barber> findAllByBarbershopId(UUID barbershopId);
}
