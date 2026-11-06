package br.com.bartzeit.identity.api;

import br.com.bartzeit.identity.application.TenantUserService;
import br.com.bartzeit.identity.domain.PasswordPolicy;
import br.com.bartzeit.identity.domain.TenantUserPrincipal;
import br.com.bartzeit.shared.tenant.TenantAccessService;
import br.com.bartzeit.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/barbershops/{tenantSlug}/users")
public class TenantUserController {
    private final TenantUserService tenantUserService;
    private final TenantAccessService tenantAccessService;

    public TenantUserController(TenantUserService tenantUserService, TenantAccessService tenantAccessService) {
        this.tenantUserService = tenantUserService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public List<TenantUserService.UserResponse> listUsers(
            @PathVariable String tenantSlug, @AuthenticationPrincipal TenantUserPrincipal principal) {
        TenantContext tenant = tenantAccessService.requireTenant(tenantSlug, principal);
        return tenantUserService.listUsers(tenant);
    }

    @PostMapping
    public ResponseEntity<TenantUserService.UserResponse> createStaff(
            @PathVariable String tenantSlug,
            @AuthenticationPrincipal TenantUserPrincipal principal,
            @Valid @RequestBody CreateStaffRequest request) {
        TenantContext tenant = tenantAccessService.requireTenant(tenantSlug, principal);
        TenantUserService.UserResponse response = tenantUserService.createStaff(tenant, request.toServiceRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public record CreateStaffRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password
    ) {
        @AssertTrue(message = "Password exceeds the supported length.")
        public boolean isPasswordLengthValid() {
            return PasswordPolicy.isWithinBcryptLimit(password);
        }

        private TenantUserService.CreateStaffRequest toServiceRequest() {
            return new TenantUserService.CreateStaffRequest(name, email, password);
        }
    }
}
