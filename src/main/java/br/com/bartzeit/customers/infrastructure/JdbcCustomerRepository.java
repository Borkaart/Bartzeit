package br.com.bartzeit.customers.infrastructure;

import br.com.bartzeit.customers.domain.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCustomerRepository implements CustomerRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Customer save(Customer customer) {
        int updated = jdbcTemplate.update("""
                UPDATE customers SET name = ?, email = ?, phone = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND tenant_id = ?
                """, customer.getName(), customer.getEmail(), customer.getPhone(), customer.getId(), customer.getBarbershopId());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO customers (id, tenant_id, name, email, phone)
                    VALUES (?, ?, ?, ?, ?)
                    """, customer.getId(), customer.getBarbershopId(), customer.getName(), customer.getEmail(), customer.getPhone());
        }
        return customer;
    }

    @Override
    public Optional<Customer> findByIdAndBarbershopId(UUID id, UUID barbershopId) {
        return jdbcTemplate.query("SELECT * FROM customers WHERE id = ? AND tenant_id = ?", this::mapOne, id, barbershopId)
                .stream().findFirst();
    }

    @Override
    public List<Customer> findAllByBarbershopId(UUID barbershopId) {
        return jdbcTemplate.query("SELECT * FROM customers WHERE tenant_id = ? ORDER BY name", this::mapOne, barbershopId);
    }

    private Customer mapOne(ResultSet rs, int rowNum) throws SQLException {
        return new Customer(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone")
        );
    }
}
