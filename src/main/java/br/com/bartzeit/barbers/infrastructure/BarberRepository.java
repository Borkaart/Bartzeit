package br.com.bartzeit.barbers.infrastructure;

import br.com.bartzeit.barbers.domain.Barber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BarberRepository {
    Barber save(Barber barber);
    Optional<Barber> findById(UUID id);
    Optional<Barber> findByIdAndBarbershopId(UUID id, UUID barbershopId);
    List<Barber> findAllByBarbershopId(UUID barbershopId);
}
