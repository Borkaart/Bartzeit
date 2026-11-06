package br.com.bartzeit.identity.application;

import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.identity.domain.UserRole;
import br.com.bartzeit.identity.infrastructure.UserRepository;
import br.com.bartzeit.shared.error.ConflictException;
import br.com.bartzeit.shared.tenant.TenantResolver;
import br.com.bartzeit.tenants.domain.Barbershop;
import br.com.bartzeit.tenants.infrastructure.BarbershopRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class OnboardingService {
    private final BarbershopRepository barbershopRepository;
    private final UserRepository userRepository;
    private final TenantResolver tenantResolver;
    private final PasswordEncoder passwordEncoder;

    public OnboardingService(BarbershopRepository barbershopRepository, UserRepository userRepository,
                             TenantResolver tenantResolver, PasswordEncoder passwordEncoder) {
        this.barbershopRepository = barbershopRepository;
        this.userRepository = userRepository;
        this.tenantResolver = tenantResolver;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OnboardingResult register(OnboardingRequest request) {
        String slug = tenantResolver.resolve(request.slug());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (barbershopRepository.findBySlug(slug).isPresent() || userRepository.existsByEmail(email)) {
            throw new ConflictException("The tenant slug or email is already registered.");
        }

        Barbershop tenant = new Barbershop(UUID.randomUUID(), request.name().trim(), slug, request.city().trim(),
                request.address().trim(), request.phone(), true);
        TenantUserPrincipal owner = new TenantUserPrincipal(UUID.randomUUID(), tenant.getId(), slug,
                request.ownerName().trim(), email, passwordEncoder.encode(request.password()), UserRole.OWNER, true);

        try {
            barbershopRepository.save(tenant);
            userRepository.save(owner);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("The tenant slug or email is already registered.");
        }
        return new OnboardingResult(tenant.getId(), tenant.getSlug(), tenant.getName(),
                owner.id(), owner.name(), owner.getUsername(), owner.role());
    }

    public record OnboardingRequest(String name, String slug, String city, String address, String phone,
                                    String ownerName, String email, String password) {
    }

    public record OnboardingResult(UUID tenantId, String tenantSlug, String tenantName,
                                   UUID userId, String name, String email, UserRole role) {
    }
}
