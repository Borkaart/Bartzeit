package br.com.bartzeit.services.infrastructure;

import br.com.bartzeit.services.domain.ServiceOffering;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository {
    ServiceOffering save(ServiceOffering offering);
    Optional<ServiceOffering> findByIdAndBarbershopId(UUID id, UUID barbershopId);
    List<ServiceOffering> findAllByBarbershopId(UUID barbershopId);
}
