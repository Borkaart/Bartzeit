package br.com.bartzeit.appointments.infrastructure;

import br.com.bartzeit.appointments.domain.Appointment;
import br.com.bartzeit.appointments.domain.AppointmentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAppointmentRepository implements AppointmentRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Appointment save(Appointment appointment) {
        int updated = jdbcTemplate.update("""
                UPDATE appointments SET service_id = ?, barber_id = ?, customer_id = ?, customer_name = ?,
                    starts_at = ?, ends_at = ?, duration_minutes = ?, status = ?, notes = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND tenant_id = ?
                """, appointment.getServiceId(), appointment.getBarberId(), appointment.getCustomerId(),
                appointment.getCustomerName(), appointment.getStartsAt(), appointment.getEndsAt(),
                appointment.getDurationMinutes(), appointment.getStatus().name(), appointment.getNotes(),
                appointment.getId(), appointment.getBarbershopId());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO appointments (id, tenant_id, service_id, barber_id, customer_id, customer_name,
                        starts_at, ends_at, duration_minutes, status, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, appointment.getId(), appointment.getBarbershopId(), appointment.getServiceId(),
                    appointment.getBarberId(), appointment.getCustomerId(), appointment.getCustomerName(),
                    appointment.getStartsAt(), appointment.getEndsAt(), appointment.getDurationMinutes(),
                    appointment.getStatus().name(), appointment.getNotes());
        }
        return appointment;
    }

    @Override
    public Optional<Appointment> findByIdAndBarbershopId(UUID id, UUID barbershopId) {
        return jdbcTemplate.query("SELECT * FROM appointments WHERE id = ? AND tenant_id = ?", this::mapOne, id, barbershopId)
                .stream().findFirst();
    }

    @Override
    public List<Appointment> findBetween(UUID barbershopId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
                SELECT * FROM appointments
                WHERE tenant_id = ? AND starts_at >= ? AND starts_at < ?
                ORDER BY starts_at
                """, this::mapOne, barbershopId, from, to);
    }

    @Override
    public List<Appointment> findByBarberBetween(UUID barbershopId, UUID barberId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
                SELECT * FROM appointments
                WHERE tenant_id = ? AND barber_id = ? AND starts_at >= ? AND starts_at < ?
                ORDER BY starts_at
                """, this::mapOne, barbershopId, barberId, from, to);
    }

    @Override
    public boolean hasOverlap(UUID barbershopId, UUID barberId, LocalDateTime startsAt, LocalDateTime endsAt,
                              UUID excludedAppointmentId) {
        String sql = """
                SELECT COUNT(*) FROM appointments
                WHERE tenant_id = ? AND barber_id = ? AND status <> ?
                    AND starts_at < ? AND ends_at > ?
                """ + (excludedAppointmentId == null ? "" : " AND id <> ?");
        Integer matches;
        if (excludedAppointmentId == null) {
            matches = jdbcTemplate.queryForObject(sql, Integer.class, barbershopId, barberId,
                    AppointmentStatus.CANCELLED.name(), endsAt, startsAt);
        } else {
            matches = jdbcTemplate.queryForObject(sql, Integer.class, barbershopId, barberId,
                    AppointmentStatus.CANCELLED.name(), endsAt, startsAt, excludedAppointmentId);
        }
        return matches != null && matches > 0;
    }

    private Appointment mapOne(ResultSet rs, int rowNum) throws SQLException {
        return new Appointment(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("service_id", UUID.class),
                rs.getObject("barber_id", UUID.class),
                rs.getObject("customer_id", UUID.class),
                rs.getString("customer_name"),
                rs.getObject("starts_at", LocalDateTime.class),
                rs.getInt("duration_minutes"),
                AppointmentStatus.valueOf(rs.getString("status")),
                rs.getString("notes")
        );
    }
}
