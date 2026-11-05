package br.com.bartzeit.appointments.infrastructure;

import br.com.bartzeit.appointments.domain.Appointment;
import br.com.bartzeit.appointments.domain.AppointmentStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAppointmentRepository implements AppointmentRepository {
    private final Map<UUID, Appointment> stores = new ConcurrentHashMap<>();

    @Override
    public Appointment save(Appointment appointment) {
        stores.put(appointment.getId(), appointment);
        return appointment;
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return Optional.ofNullable(stores.get(id));
    }

    @Override
    public List<Appointment> findAllByBarbershopId(UUID barbershopId) {
        return stores.values().stream()
                .filter(appointment -> appointment.getBarbershopId().equals(barbershopId))
                .sorted(Comparator.comparing(Appointment::getStartsAt))
                .toList();
    }

    @Override
    public List<Appointment> findAllByBarberId(UUID barberId) {
        return stores.values().stream()
                .filter(appointment -> appointment.getBarberId() != null && appointment.getBarberId().equals(barberId))
                .sorted(Comparator.comparing(Appointment::getStartsAt))
                .toList();
    }

    @Override
    public List<Appointment> findBetween(UUID barbershopId, LocalDateTime from, LocalDateTime to) {
        return stores.values().stream()
                .filter(appointment -> appointment.getBarbershopId().equals(barbershopId))
                .filter(appointment -> !appointment.getStartsAt().isBefore(from))
                .filter(appointment -> appointment.getStartsAt().isBefore(to))
                .sorted(Comparator.comparing(Appointment::getStartsAt))
                .toList();
    }

    @Override
    public List<Appointment> findByBarberBetween(UUID barberId, LocalDateTime from, LocalDateTime to) {
        return stores.values().stream()
                .filter(appointment -> appointment.getBarberId() != null && appointment.getBarberId().equals(barberId))
                .filter(appointment -> !appointment.getStartsAt().isBefore(from))
                .filter(appointment -> appointment.getStartsAt().isBefore(to))
                .sorted(Comparator.comparing(Appointment::getStartsAt))
                .toList();
    }

    @Override
    public boolean hasOverlap(UUID barberId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return stores.values().stream()
                .filter(appointment -> appointment.getBarberId() != null && appointment.getBarberId().equals(barberId))
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
                .anyMatch(appointment -> appointment.getStartsAt().isBefore(endsAt) && startsAt.isBefore(appointment.getEndsAt()));
    }
}
