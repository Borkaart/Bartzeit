CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE,
    city VARCHAR(120) NOT NULL,
    address VARCHAR(240) NOT NULL,
    phone VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(24) NOT NULL CHECK (role IN ('OWNER', 'STAFF')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, tenant_id)
);

CREATE INDEX idx_users_tenant_role ON users (tenant_id, role);

CREATE TABLE services (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 1 AND 480),
    price NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, tenant_id)
);

CREATE INDEX idx_services_tenant_active_name ON services (tenant_id, active, name);

CREATE TABLE barbers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    name VARCHAR(120) NOT NULL,
    specialty VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, tenant_id)
);

CREATE INDEX idx_barbers_tenant_active_name ON barbers (tenant_id, active, name);

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, tenant_id)
);

CREATE INDEX idx_customers_tenant_name ON customers (tenant_id, name);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    service_id UUID NOT NULL,
    barber_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    customer_name VARCHAR(120) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 1 AND 480),
    status VARCHAR(24) NOT NULL CHECK (status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (ends_at > starts_at),
    FOREIGN KEY (service_id, tenant_id) REFERENCES services (id, tenant_id),
    FOREIGN KEY (barber_id, tenant_id) REFERENCES barbers (id, tenant_id),
    FOREIGN KEY (customer_id, tenant_id) REFERENCES customers (id, tenant_id),
    UNIQUE (id, tenant_id)
);

CREATE INDEX idx_appointments_tenant_starts ON appointments (tenant_id, starts_at);
CREATE INDEX idx_appointments_barber_schedule ON appointments (tenant_id, barber_id, starts_at, ends_at);
CREATE INDEX idx_appointments_customer ON appointments (tenant_id, customer_id, starts_at);
