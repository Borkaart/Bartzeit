package br.com.bartzeit.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/public/barbershops",
                        "/api/public/barbershops/*",
                        "/api/public/barbershops/*/services",
                        "/api/v1/barbershops/*/availability").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/barbershops",
                        "/api/v1/barbershops/*/services",
                        "/api/v1/barbershops/*/barbers",
                        "/api/v1/barbershops/*/customers",
                        "/api/v1/barbershops/*/appointments").hasAnyRole("TENANT_ADMIN", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/barbershops/*/barbers/*/status",
                        "/api/v1/barbershops/*/appointments/*/status").hasAnyRole("TENANT_ADMIN", "PLATFORM_ADMIN", "BARBER")
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/barbershops/*/customers",
                        "/api/v1/barbershops/*/customers/*").hasAnyRole("TENANT_ADMIN", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/barbershops/*/appointments",
                        "/api/v1/barbershops/*/appointments/*",
                        "/api/v1/barbershops/*/barbers/*/appointments").hasAnyRole("TENANT_ADMIN", "PLATFORM_ADMIN", "BARBER")
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/barbershops/*/barbers",
                        "/api/v1/barbershops/*/barbers/*").hasAnyRole("TENANT_ADMIN", "PLATFORM_ADMIN", "BARBER")
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/barbershops/*/services",
                        "/api/v1/barbershops/*/services/*").hasAnyRole("TENANT_ADMIN", "PLATFORM_ADMIN", "BARBER")
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/barbershops",
                        "/api/v1/barbershops/*").hasAnyRole("TENANT_ADMIN", "PLATFORM_ADMIN")
                .anyRequest().denyAll()
            )
            .httpBasic(Customizer.withDefaults())
            .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
