package br.com.bartzeit.barbers.infrastructure;

import br.com.bartzeit.barbers.domain.Barber;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcBarberRepository implements BarberRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcBarberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Barber save(Barber barber) {
        int updated = jdbcTemplate.update("""
                UPDATE barbers SET name = ?, specialty = ?, active = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND tenant_id = ?
                """, barber.getName(), barber.getSpecialty(), barber.isActive(), barber.getId(), barber.getBarbershopId());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO barbers (id, tenant_id, name, specialty, active)
                    VALUES (?, ?, ?, ?, ?)
                    """, barber.getId(), barber.getBarbershopId(), barber.getName(), barber.getSpecialty(), barber.isActive());
        }
        return barber;
    }

    @Override
    public Optional<Barber> findByIdAndBarbershopId(UUID id, UUID barbershopId) {
        return jdbcTemplate.query("SELECT * FROM barbers WHERE id = ? AND tenant_id = ?", this::mapOne, id, barbershopId)
                .stream().findFirst();
    }

    @Override
    public List<Barber> findAllByBarbershopId(UUID barbershopId) {
        return jdbcTemplate.query("SELECT * FROM barbers WHERE tenant_id = ? ORDER BY name", this::mapOne, barbershopId);
    }

    @Override
    public Optional<Barber> lockByIdAndBarbershopId(UUID id, UUID barbershopId) {
        return jdbcTemplate.query("SELECT * FROM barbers WHERE id = ? AND tenant_id = ? FOR UPDATE",
                this::mapOne, id, barbershopId).stream().findFirst();
    }

    private Barber mapOne(ResultSet rs, int rowNum) throws SQLException {
        return new Barber(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("name"),
                rs.getString("specialty"),
                rs.getBoolean("active")
        );
    }
}
