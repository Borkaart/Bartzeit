package br.com.bartzeit.services.infrastructure;

import br.com.bartzeit.services.domain.ServiceOffering;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public class JdbcServiceRepository implements ServiceRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcServiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ServiceOffering save(ServiceOffering offering) {
        int updated = jdbcTemplate.update("""
                UPDATE services SET name = ?, description = ?, duration_minutes = ?, price = ?, active = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND tenant_id = ?
                """, offering.getName(), offering.getDescription(), offering.getDurationMinutes(), offering.getPrice(),
                offering.isActive(), offering.getId(), offering.getBarbershopId());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO services (id, tenant_id, name, description, duration_minutes, price, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, offering.getId(), offering.getBarbershopId(), offering.getName(), offering.getDescription(),
                    offering.getDurationMinutes(), offering.getPrice(), offering.isActive());
        }
        return offering;
    }

    @Override
    public Optional<ServiceOffering> findByIdAndBarbershopId(UUID id, UUID barbershopId) {
        return jdbcTemplate.query("SELECT * FROM services WHERE id = ? AND tenant_id = ?", this::mapOne, id, barbershopId)
                .stream().findFirst();
    }

    @Override
    public List<ServiceOffering> findAllByBarbershopId(UUID barbershopId) {
        return jdbcTemplate.query("SELECT * FROM services WHERE tenant_id = ? ORDER BY name", this::mapOne, barbershopId);
    }

    private ServiceOffering mapOne(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceOffering(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("duration_minutes"),
                rs.getBigDecimal("price"),
                rs.getBoolean("active")
        );
    }
}
