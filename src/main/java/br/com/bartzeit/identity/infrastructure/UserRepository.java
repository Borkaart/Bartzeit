package br.com.bartzeit.identity.infrastructure;

import br.com.bartzeit.identity.domain.TenantUserPrincipal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    TenantUserPrincipal save(TenantUserPrincipal user);
    Optional<TenantUserPrincipal> findByEmail(String email);
    Optional<TenantUserPrincipal> findByIdAndTenantId(UUID id, UUID tenantId);
    List<TenantUserPrincipal> findAllByTenantId(UUID tenantId);
    boolean existsByEmail(String email);
}
