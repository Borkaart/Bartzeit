package br.com.bartzeit.appointments.infrastructure;

import br.com.bartzeit.appointments.domain.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    Appointment save(Appointment appointment);
    Optional<Appointment> findByIdAndBarbershopId(UUID id, UUID barbershopId);
    List<Appointment> findBetween(UUID barbershopId, LocalDateTime from, LocalDateTime to);
    List<Appointment> findByBarberBetween(UUID barbershopId, UUID barberId, LocalDateTime from, LocalDateTime to);
    boolean hasOverlap(UUID barbershopId, UUID barberId, LocalDateTime startsAt, LocalDateTime endsAt,
                       UUID excludedAppointmentId);
}
