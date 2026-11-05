package br.com.bartzeit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void allowsOnlyConfirmedMarketplaceReadsWithoutAuthentication() throws Exception {
        String slug = createCatalogFixture();
        String serviceId = getServiceId(slug);

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/barbershops"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/barbershops/{slug}", slug))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/barbershops/{slug}/services", slug))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/availability", slug)
                        .param("date", LocalDate.now().plusDays(1).toString())
                        .param("serviceId", serviceId))
                .andExpect(status().isOk());
    }

    @Test
    void deniesAnonymousAccessToPrivateCatalogCustomerAndBarberReads() throws Exception {
        String slug = createCatalogFixture();
        String serviceId = getServiceId(slug);
        String barberId = getBarberId(slug);

        mockMvc.perform(get("/api/v1/barbershops"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}", slug))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/services", slug))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/services/{serviceId}", slug, serviceId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/barbers", slug))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/barbers/{barberId}", slug, barberId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/customers", slug))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/customers/{customerId}", slug, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesAnonymousAccessToAppointmentQueries() throws Exception {
        String slug = createCatalogFixture();
        String barberId = getBarberId(slug);

        mockMvc.perform(get("/api/v1/barbershops/{slug}/appointments", slug))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/appointments/{appointmentId}", slug, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/barbers/{barberId}/appointments", slug, barberId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnclassifiedRoutesAndWritesWithoutAuthorization() throws Exception {
        String slug = createCatalogFixture();
        String barberId = getBarberId(slug);

        mockMvc.perform(get("/api/v1/barbershops/{slug}/private-report", slug))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/public/barbershops/{slug}/customers", slug))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/barbershops/{slug}/private-report", slug)
                        .with(user("test-admin").roles("TENANT_ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/barbershops/{slug}/unknown-write", slug)
                        .with(user("test-admin").roles("TENANT_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/barbershops/{slug}/appointments", slug)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/barbershops")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/barbershops/{slug}/services", slug)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/barbershops/{slug}/barbers", slug)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/barbershops/{slug}/customers", slug)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/barbershops/{slug}/barbers/{barberId}/status", slug, barberId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/barbershops/{slug}/appointments/{appointmentId}/status", slug, UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/barbershops/{slug}/appointments/{appointmentId}", slug, UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/barbershops/{slug}/customers/{customerId}", slug, UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAuthenticatedCustomerRoleAndRequiresCsrfForWrites() throws Exception {
        String slug = createCatalogFixture();

        mockMvc.perform(get("/api/v1/barbershops/{slug}/customers", slug)
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/barbershops/{slug}/services", slug)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New service\",\"durationMinutes\":30,\"price\":20}"))
                .andExpect(status().isForbidden());
    }

    private String createCatalogFixture() throws Exception {
        String slug = "security-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/v1/barbershops")
                        .with(user("test-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Security Test Shop","slug":"%s","city":"Test City","address":"Test Street"}
                                """.formatted(slug)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/barbershops/{slug}/services", slug)
                        .with(user("test-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Security Test Service","durationMinutes":30,"price":25}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/barbershops/{slug}/barbers", slug)
                        .with(user("test-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Security Test Barber"}
                                """))
                .andExpect(status().isCreated());

        return slug;
    }

    private String getServiceId(String slug) throws Exception {
        String response = mockMvc.perform(get("/api/v1/barbershops/{slug}/services", slug)
                        .with(user("test-admin").roles("TENANT_ADMIN")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get(0).get("id").asText();
    }

    private String getBarberId(String slug) throws Exception {
        String response = mockMvc.perform(get("/api/v1/barbershops/{slug}/barbers", slug)
                        .with(user("test-admin").roles("TENANT_ADMIN")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get(0).get("id").asText();
    }
}
