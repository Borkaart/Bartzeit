package br.com.bartzeit;

import br.com.bartzeit.appointments.domain.Appointment;
import br.com.bartzeit.appointments.domain.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentStatusRulesTest {

    @Test
    void shouldAllowValidTransitionsAndRejectInvalidOnes() {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        assertTrue(appointment.canTransitionTo(AppointmentStatus.IN_PROGRESS));
        assertTrue(appointment.canTransitionTo(AppointmentStatus.CANCELLED));
        assertFalse(appointment.canTransitionTo(AppointmentStatus.CONFIRMED));

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        assertTrue(appointment.canTransitionTo(AppointmentStatus.COMPLETED));
        assertFalse(appointment.canTransitionTo(AppointmentStatus.CONFIRMED));

        appointment.setStatus(AppointmentStatus.COMPLETED);
        assertFalse(appointment.canTransitionTo(AppointmentStatus.CANCELLED));
    }

    @Test
    void shouldComputeAppointmentWindow() {
        Appointment appointment = new Appointment();
        LocalDateTime startsAt = LocalDateTime.of(2026, 11, 20, 10, 0);
        appointment.setStartsAt(startsAt);
        appointment.setDurationMinutes(45);

        assertEquals(startsAt.plusMinutes(45), appointment.getEndsAt());
    }
}
