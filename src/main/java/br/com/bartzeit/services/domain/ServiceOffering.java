package br.com.bartzeit.services.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class ServiceOffering {
    private UUID id;
    private UUID barbershopId;
    private String name;
    private String description;
    private int durationMinutes;
    private BigDecimal price;
    private boolean active = true;

    public ServiceOffering() {
    }

    public ServiceOffering(UUID id, UUID barbershopId, String name, String description, int durationMinutes, BigDecimal price, boolean active) {
        this.id = id;
        this.barbershopId = barbershopId;
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.active = active;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceOffering that = (ServiceOffering) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
