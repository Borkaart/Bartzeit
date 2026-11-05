package br.com.bartzeit.appointments.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Appointment {
    private UUID id;
    private UUID barbershopId;
    private UUID serviceId;
    private UUID barberId;
    private UUID customerId;
    private String customerName;
    private LocalDateTime startsAt;
    private int durationMinutes;
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;
    private String notes;

    public Appointment() {
    }

    public Appointment(UUID id, UUID barbershopId, UUID serviceId, UUID barberId, UUID customerId, String customerName,
                       LocalDateTime startsAt, int durationMinutes, AppointmentStatus status, String notes) {
        this.id = id;
        this.barbershopId = barbershopId;
        this.serviceId = serviceId;
        this.barberId = barberId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.startsAt = startsAt;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBarbershopId() {
        return barbershopId;
    }

    public void setBarbershopId(UUID barbershopId) {
        this.barbershopId = barbershopId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getBarberId() {
        return barberId;
    }

    public void setBarberId(UUID barberId) {
        this.barberId = barberId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getEndsAt() {
        return startsAt.plusMinutes(durationMinutes);
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        if (target == null || target == status) {
            return false;
        }
        return switch (status) {
            case CONFIRMED -> target == AppointmentStatus.IN_PROGRESS || target == AppointmentStatus.COMPLETED || target == AppointmentStatus.CANCELLED;
            case IN_PROGRESS -> target == AppointmentStatus.COMPLETED || target == AppointmentStatus.CANCELLED;
            default -> false;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Appointment that = (Appointment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
