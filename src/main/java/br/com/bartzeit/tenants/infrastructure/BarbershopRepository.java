package br.com.bartzeit.tenants.infrastructure;

import br.com.bartzeit.tenants.domain.Barbershop;

import java.util.Optional;
import java.util.UUID;

public interface BarbershopRepository {
    Barbershop save(Barbershop barbershop);
    Optional<Barbershop> findById(UUID id);
    Optional<Barbershop> findBySlug(String slug);
}
