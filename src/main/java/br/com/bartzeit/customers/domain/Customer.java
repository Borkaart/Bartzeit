package br.com.bartzeit.customers.domain;

import java.util.Objects;
import java.util.UUID;

public class Customer {
    private UUID id;
    private UUID barbershopId;
    private String name;
    private String email;
    private String phone;

    public Customer() {
    }

    public Customer(UUID id, UUID barbershopId, String name, String email, String phone) {
        this.id = id;
        this.barbershopId = barbershopId;
        this.name = name;
        this.email = email;
        this.phone = phone;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
