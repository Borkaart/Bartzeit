package br.com.bartzeit.barbers.domain;

import java.util.Objects;
import java.util.UUID;

public class Barber {
    private UUID id;
    private UUID barbershopId;
    private String name;
    private String specialty;
    private boolean active = true;

    public Barber() {
    }

    public Barber(UUID id, UUID barbershopId, String name, String specialty, boolean active) {
        this.id = id;
        this.barbershopId = barbershopId;
        this.name = name;
        this.specialty = specialty;
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

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
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
        Barber barber = (Barber) o;
        return Objects.equals(id, barber.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
