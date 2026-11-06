package br.com.bartzeit.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                  CorsConfigurationSource corsConfigurationSource,
                                                  CsrfTokenRepository csrfTokenRepository,
                                                  SecurityContextRepository securityContextRepository,
                                                  LogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/onboarding", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/barbershops/*/users").hasRole("OWNER")
                .requestMatchers(HttpMethod.POST, "/api/v1/barbershops/*/users").hasRole("OWNER")
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/barbershops/*/services",
                        "/api/v1/barbershops/*/barbers").hasRole("OWNER")
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/barbershops/*/customers",
                        "/api/v1/barbershops/*/appointments").hasAnyRole("OWNER", "STAFF")
                .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/barbershops/*/barbers/*/status",
                        "/api/v1/barbershops/*/appointments/*/status").hasAnyRole("OWNER", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/v1/barbershops/*/appointments/*").hasAnyRole("OWNER", "STAFF")
                .requestMatchers(HttpMethod.GET, "/api/v1/barbershops/**").hasAnyRole("OWNER", "STAFF")
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .anyRequest().denyAll()
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler(logoutSuccessHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-CSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
