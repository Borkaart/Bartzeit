package br.com.bartzeit.identity.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class TenantUserPrincipal implements UserDetails {
    private final UUID id;
    private final UUID tenantId;
    private final String tenantSlug;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean active;

    public TenantUserPrincipal(UUID id, UUID tenantId, String tenantSlug, String name, String email,
                               String passwordHash, UserRole role, boolean active) {
        this.id = id;
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String tenantSlug() {
        return tenantSlug;
    }

    public String name() {
        return name;
    }

    public UserRole role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
