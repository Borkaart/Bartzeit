package br.com.bartzeit.appointments.infrastructure;

import br.com.bartzeit.appointments.domain.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(UUID id);
    List<Appointment> findAllByBarbershopId(UUID barbershopId);
    List<Appointment> findAllByBarberId(UUID barberId);
    List<Appointment> findBetween(UUID barbershopId, LocalDateTime from, LocalDateTime to);
    List<Appointment> findByBarberBetween(UUID barberId, LocalDateTime from, LocalDateTime to);
    boolean hasOverlap(UUID barberId, LocalDateTime startsAt, LocalDateTime endsAt);
}
