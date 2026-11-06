package br.com.bartzeit.tenants.infrastructure;

import br.com.bartzeit.tenants.domain.Barbershop;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcBarbershopRepository implements BarbershopRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcBarbershopRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Barbershop save(Barbershop barbershop) {
        jdbcTemplate.update("""
                INSERT INTO tenants (id, name, slug, city, address, phone, active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, barbershop.getId(), barbershop.getName(), barbershop.getSlug(),
                barbershop.getCity(), barbershop.getAddress(), barbershop.getPhone(), barbershop.isActive());
        return barbershop;
    }

    @Override
    public Optional<Barbershop> findById(UUID id) {
        return jdbcTemplate.query("SELECT * FROM tenants WHERE id = ?", this::mapOne, id).stream().findFirst();
    }

    @Override
    public Optional<Barbershop> findBySlug(String slug) {
        return jdbcTemplate.query("SELECT * FROM tenants WHERE slug = ?", this::mapOne, slug).stream().findFirst();
    }

    private Barbershop mapOne(ResultSet rs, int rowNum) throws SQLException {
        return new Barbershop(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("city"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getBoolean("active")
        );
    }
}
