package br.com.bartzeit;

import br.com.bartzeit.identity.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaaSMvpIntegrationTest {
    private static final String PASSWORD = "Example-Password-58!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void onlyBootstrapAndHealthRoutesArePublicAndLoginErrorsDoNotEchoCredentials() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/barbershops")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/public/barbershops")).andExpect(status().isUnauthorized());

        CsrfSession csrf = newCsrfSession();
        MvcResult failedLogin = mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType("application/json")
                        .content("""
                                {"email":"missing@example.test","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertFalse(failedLogin.getResponse().getContentAsString().contains(PASSWORD));
    }

    @Test
    void onboardingPersistsHashedOwnerAndLoginCreatesAuthenticatedSession() throws Exception {
        RegisteredTenant tenant = registerTenant();
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, tenant.email());

        assertNotNull(storedHash);
        assertNotEquals(PASSWORD, storedHash);
        assertTrue(storedHash.startsWith("$2a$"));

        MockHttpSession session = login(tenant.email());
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(tenant.email()))
                .andExpect(jsonPath("$.role").value(UserRole.OWNER.name()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(get("/api/v1/barbershops").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(tenant.slug()));
    }

    @Test
    void staffRoleIsScopedAndCannotManageOwnerOnlyResources() throws Exception {
        RegisteredTenant tenant = registerTenant();
        MockHttpSession ownerSession = login(tenant.email());
        String staffEmail = "staff-" + UUID.randomUUID() + "@example.test";

        mockMvc.perform(post("/api/v1/barbershops/{slug}/users", tenant.slug())
                        .session(ownerSession)
                        .header("X-CSRF-TOKEN", csrfFor(ownerSession))
                        .contentType("application/json")
                        .content("""
                                {"name":"Test Staff","email":"%s","password":"%s"}
                                """.formatted(staffEmail, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value(UserRole.STAFF.name()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        MockHttpSession staffSession = login(staffEmail);
        mockMvc.perform(post("/api/v1/barbershops/{slug}/services", tenant.slug())
                        .session(staffSession)
                        .header("X-CSRF-TOKEN", csrfFor(staffSession))
                        .contentType("application/json")
                        .content("""
                                {"name":"Unauthorized Service","durationMinutes":30,"price":25}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/barbershops/{slug}/users", tenant.slug()).session(staffSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void businessFlowsPersistAndCannotCrossTenantBoundaries() throws Exception {
        RegisteredTenant tenantA = registerTenant();
        RegisteredTenant tenantB = registerTenant();
        MockHttpSession sessionA = login(tenantA.email());
        MockHttpSession sessionB = login(tenantB.email());
        String serviceId = createService(sessionA, tenantA.slug());
        String barberId = createBarber(sessionA, tenantA.slug());

        mockMvc.perform(get("/api/v1/barbershops/{slug}/services/{id}", tenantB.slug(), serviceId)
                        .session(sessionB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/services", tenantB.slug())
                        .session(sessionA))
                .andExpect(status().isNotFound());

        String startsAt = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0)
                .withSecond(0).withNano(0).toString();
        String appointmentJson = """
                {"serviceId":"%s","barberId":"%s","customerName":"Example Customer","startsAt":"%s"}
                """.formatted(serviceId, barberId, startsAt);
        String appointmentId = objectMapper.readTree(mockMvc.perform(post("/api/v1/barbershops/{slug}/appointments", tenantA.slug())
                        .session(sessionA)
                        .header("X-CSRF-TOKEN", csrfFor(sessionA))
                        .contentType("application/json")
                        .content(appointmentJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/barbershops/{slug}/appointments", tenantA.slug())
                        .session(sessionA)
                        .header("X-CSRF-TOKEN", csrfFor(sessionA))
                        .contentType("application/json")
                        .content(appointmentJson))
                .andExpect(status().isConflict());
        String movedTo = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0)
                .withSecond(0).withNano(0).toString();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/v1/barbershops/{slug}/appointments/{id}", tenantA.slug(), appointmentId)
                        .session(sessionA)
                        .header("X-CSRF-TOKEN", csrfFor(sessionA))
                        .contentType("application/json")
                        .content("""
                                {"serviceId":"%s","barberId":"%s","startsAt":"%s","notes":"Updated example"}
                                """.formatted(serviceId, barberId, movedTo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsAt").value(movedTo + ":00"))
                .andExpect(jsonPath("$.notes").value("Updated example"));
        mockMvc.perform(get("/api/v1/barbershops/{slug}/appointments", tenantB.slug()).session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/appointments/{id}", tenantB.slug(), appointmentId)
                        .session(sessionB))
                .andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/v1/barbershops/{slug}/appointments/{id}", tenantB.slug(), appointmentId)
                        .session(sessionB)
                        .header("X-CSRF-TOKEN", csrfFor(sessionB))
                        .contentType("application/json")
                        .content("""
                                {"serviceId":"%s","barberId":"%s","startsAt":"%s","notes":"Cross-tenant attempt"}
                                """.formatted(serviceId, barberId, startsAt)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/barbershops/{slug}/appointments/{id}/status", tenantB.slug(), appointmentId)
                        .session(sessionB)
                        .header("X-CSRF-TOKEN", csrfFor(sessionB))
                        .contentType("application/json")
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/barbershops/{slug}/appointments/{id}/status", tenantA.slug(), appointmentId)
                        .session(sessionA)
                        .header("X-CSRF-TOKEN", csrfFor(sessionA))
                        .contentType("application/json")
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Integer appointmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE tenant_id = ?", Integer.class, tenantA.tenantId());
        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE tenant_id = ?", Integer.class, tenantA.tenantId());
        assertEquals(1, appointmentCount);
        assertEquals(1, customerCount);
    }

    private RegisteredTenant registerTenant() throws Exception {
        String slug = "shop-" + UUID.randomUUID().toString().substring(0, 8);
        String email = "owner-" + UUID.randomUUID() + "@example.test";
        CsrfSession csrf = newCsrfSession();
        MvcResult result = mockMvc.perform(post("/api/auth/onboarding")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType("application/json")
                        .content("""
                                {"name":"Example Shop","slug":"%s","city":"Test City","address":"10 Example Street",
                                 "ownerName":"Example Owner","email":"%s","password":"%s"}
                                """.formatted(slug, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new RegisteredTenant(response.get("tenantId").asText(), slug, email);
    }

    private MockHttpSession login(String email) throws Exception {
        CsrfSession csrf = newCsrfSession();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private CsrfSession newCsrfSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return new CsrfSession(session, token);
    }

    private String csrfFor(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String createService(MockHttpSession session, String slug) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/barbershops/{slug}/services", slug)
                        .session(session)
                        .header("X-CSRF-TOKEN", csrfFor(session))
                        .contentType("application/json")
                        .content("""
                                {"name":"Example Cut","description":"Example service","durationMinutes":30,"price":25.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createBarber(MockHttpSession session, String slug) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/barbershops/{slug}/barbers", slug)
                        .session(session)
                        .header("X-CSRF-TOKEN", csrfFor(session))
                        .contentType("application/json")
                        .content("{\"name\":\"Example Barber\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private record RegisteredTenant(String tenantId, String slug, String email) {
    }

    private record CsrfSession(MockHttpSession session, String token) {
    }
}
