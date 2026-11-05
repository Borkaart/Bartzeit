package br.com.bartzeit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BartzeitBackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndQueryBarbershop() throws Exception {
        String tenantSlug = "downtown";
        mockMvc.perform(post("/api/v1/barbershops")
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Downtown Barber\",\"slug\":\"" + tenantSlug + "\",\"city\":\"Sao Paulo\",\"address\":\"Main St 123\",\"phone\":\"1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(tenantSlug));

        mockMvc.perform(get("/api/public/barbershops/{tenantSlug}", tenantSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Downtown Barber"));
    }

    @Test
    void shouldRejectInvalidBarbershopSlug() throws Exception {
        mockMvc.perform(post("/api/v1/barbershops")
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad Barbershop\",\"slug\":\"Bad_Slug\",\"city\":\"SP\",\"address\":\"Main St\",\"phone\":\"1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateServiceAndBarberForTenant() throws Exception {
        String tenantSlug = "midtown";
        createBarbershop(tenantSlug);

        mockMvc.perform(post("/api/v1/barbershops/{tenantSlug}/services", tenantSlug)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Classic Cut\",\"description\":\"Men haircut\",\"durationMinutes\":45,\"price\":\"65.00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Classic Cut"));

        mockMvc.perform(post("/api/v1/barbershops/{tenantSlug}/barbers", tenantSlug)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex\",\"specialty\":\"Fade\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alex"));
    }

    @Test
    void shouldBlockAppointmentConflictAndValidateStatusTransitions() throws Exception {
        String tenantSlug = "harbor";
        createBarbershop(tenantSlug);
        String serviceId = createService(tenantSlug, "Beard Trim", 60, "50.00");
        String barberId = createBarber(tenantSlug, "Jordan");

        String time = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0).toString();

        mockMvc.perform(post("/api/v1/barbershops/{tenantSlug}/appointments", tenantSlug)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":\"" + serviceId + "\",\"barberId\":\"" + barberId + "\",\"customerName\":\"Maria\",\"startsAt\":\"" + time + "\",\"notes\":\"first visit\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/barbershops/{tenantSlug}/appointments", tenantSlug)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":\"" + serviceId + "\",\"barberId\":\"" + barberId + "\",\"customerName\":\"Ana\",\"startsAt\":\"" + time + "\"}"))
                .andExpect(status().isConflict());

        String appointmentId = getFirstAppointmentId(tenantSlug);
        mockMvc.perform(patch("/api/v1/barbershops/{tenantSlug}/appointments/{appointmentId}/status", tenantSlug, appointmentId)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/barbershops/{tenantSlug}/appointments/{appointmentId}/status", tenantSlug, appointmentId)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/barbershops/dummy/services")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"description\":\"Y\",\"durationMinutes\":30,\"price\":\"20.00\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPreventCrossTenantAccess() throws Exception {
        createBarbershop("tenant-a");
        createBarbershop("tenant-b");
        String serviceId = createService("tenant-a", "Premium Beard", 30, "40.00");

        mockMvc.perform(get("/api/v1/barbershops/tenant-b/services/{serviceId}", serviceId)
                        .with(user("tenant-admin").roles("TENANT_ADMIN")))
                .andExpect(status().isNotFound());
    }

    private void createBarbershop(String slug) throws Exception {
        mockMvc.perform(post("/api/v1/barbershops")
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shop %s\",\"slug\":\"" + slug + "\",\"city\":\"City\",\"address\":\"Street 1\",\"phone\":\"0000\"}".formatted(slug)))
                .andExpect(status().isCreated());
    }

    private String createService(String tenantSlug, String name, int duration, String price) throws Exception {
        String response = mockMvc.perform(post("/api/v1/barbershops/{tenantSlug}/services", tenantSlug)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"service\",\"durationMinutes\":" + duration + ",\"price\":\"" + price + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createBarber(String tenantSlug, String barberName) throws Exception {
        String response = mockMvc.perform(post("/api/v1/barbershops/{tenantSlug}/barbers", tenantSlug)
                        .with(user("tenant-admin").roles("TENANT_ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + barberName + "\",\"specialty\":\"General\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String getFirstAppointmentId(String tenantSlug) throws Exception {
        String response = mockMvc.perform(get("/api/v1/barbershops/{tenantSlug}/appointments", tenantSlug)
                        .with(user("tenant-admin").roles("TENANT_ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get(0).get("id").asText();
    }
}
