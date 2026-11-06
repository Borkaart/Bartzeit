package br.com.bartzeit.identity.application;

import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.identity.domain.UserRole;
import br.com.bartzeit.identity.infrastructure.UserRepository;
import br.com.bartzeit.shared.error.ConflictException;
import br.com.bartzeit.shared.tenant.TenantContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TenantUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createStaff(TenantContext tenant, CreateStaffRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("The email is already registered.");
        }
        TenantUserPrincipal staff = new TenantUserPrincipal(UUID.randomUUID(), tenant.barbershopId(),
                tenant.slug(), request.name().trim(), email, passwordEncoder.encode(request.password()),
                UserRole.STAFF, true);
        try {
            userRepository.save(staff);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("The email is already registered.");
        }
        return toResponse(staff);
    }

    public List<UserResponse> listUsers(TenantContext tenant) {
        return userRepository.findAllByTenantId(tenant.barbershopId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(TenantUserPrincipal user) {
        return new UserResponse(user.id(), user.name(), user.getUsername(), user.role(), user.isEnabled());
    }

    public record CreateStaffRequest(String name, String email, String password) {
    }

    public record UserResponse(UUID id, String name, String email, UserRole role, boolean active) {
    }
}
