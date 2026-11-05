package br.com.bartzeit.tenants.domain;

import java.util.Objects;
import java.util.UUID;

public class Barbershop {
    private UUID id;
    private String name;
    private String slug;
    private String city;
    private String address;
    private String phone;
    private boolean active = true;

    public Barbershop() {
    }

    public Barbershop(UUID id, String name, String slug, String city, String address, String phone, boolean active) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.city = city;
        this.address = address;
        this.phone = phone;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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
        Barbershop that = (Barbershop) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
