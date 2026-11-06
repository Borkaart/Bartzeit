package br.com.bartzeit.identity.api;

import br.com.bartzeit.identity.application.OnboardingService;
import br.com.bartzeit.identity.domain.PasswordPolicy;
import br.com.bartzeit.identity.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class OnboardingController {
    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingResponse> register(@Valid @RequestBody OnboardingRequest request) {
        OnboardingService.OnboardingResult result = onboardingService.register(request.toServiceRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(new OnboardingResponse(
                result.tenantId(), result.tenantSlug(), result.tenantName(), result.userId(),
                result.name(), result.email(), result.role()));
    }

    public record OnboardingRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max = 80) String slug,
            @NotBlank @Size(max = 120) String city,
            @NotBlank @Size(max = 240) String address,
            @Size(max = 40) String phone,
            @NotBlank @Size(max = 120) String ownerName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password
    ) {
        @AssertTrue(message = "Password exceeds the supported length.")
        public boolean isPasswordLengthValid() {
            return PasswordPolicy.isWithinBcryptLimit(password);
        }

        private OnboardingService.OnboardingRequest toServiceRequest() {
            return new OnboardingService.OnboardingRequest(name, slug, city, address, phone, ownerName, email, password);
        }
    }

    public record OnboardingResponse(UUID tenantId, String tenantSlug, String tenantName,
                                     UUID userId, String name, String email, UserRole role) {
    }
}
