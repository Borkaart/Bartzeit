package br.com.bartzeit.identity.infrastructure;

import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.identity.domain.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TenantUserPrincipal save(TenantUserPrincipal user) {
        jdbcTemplate.update("""
                INSERT INTO users (id, tenant_id, name, email, password_hash, role, active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, user.id(), user.tenantId(), user.name(), user.getUsername(), user.getPassword(),
                user.role().name(), user.isEnabled());
        return user;
    }

    @Override
    public Optional<TenantUserPrincipal> findByEmail(String email) {
        return jdbcTemplate.query("""
                SELECT users.*, tenants.slug AS tenant_slug
                FROM users JOIN tenants ON tenants.id = users.tenant_id
                WHERE users.email = ?
                """, this::mapOne, email).stream().findFirst();
    }

    @Override
    public Optional<TenantUserPrincipal> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jdbcTemplate.query("""
                SELECT users.*, tenants.slug AS tenant_slug
                FROM users JOIN tenants ON tenants.id = users.tenant_id
                WHERE users.id = ? AND users.tenant_id = ?
                """, this::mapOne, id, tenantId).stream().findFirst();
    }

    @Override
    public List<TenantUserPrincipal> findAllByTenantId(UUID tenantId) {
        return jdbcTemplate.query("""
                SELECT users.*, tenants.slug AS tenant_slug
                FROM users JOIN tenants ON tenants.id = users.tenant_id
                WHERE users.tenant_id = ? ORDER BY users.created_at
                """, this::mapOne, tenantId);
    }

    @Override
    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    private TenantUserPrincipal mapOne(ResultSet rs, int rowNum) throws SQLException {
        return new TenantUserPrincipal(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("tenant_slug"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                UserRole.valueOf(rs.getString("role")),
                rs.getBoolean("active")
        );
    }
}
