REVOKE ALL PRIVILEGES ON TABLE
    tenants,
    users,
    services,
    barbers,
    customers,
    appointments
FROM PUBLIC, anon, authenticated;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    tenants,
    users,
    services,
    barbers,
    customers,
    appointments
TO CURRENT_USER;

ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE services ENABLE ROW LEVEL SECURITY;
ALTER TABLE barbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;

CREATE POLICY bartzeit_backend_access ON tenants
    TO CURRENT_USER
    USING (true)
    WITH CHECK (true);

CREATE POLICY bartzeit_backend_access ON users
    TO CURRENT_USER
    USING (true)
    WITH CHECK (true);

CREATE POLICY bartzeit_backend_access ON services
    TO CURRENT_USER
    USING (true)
    WITH CHECK (true);

CREATE POLICY bartzeit_backend_access ON barbers
    TO CURRENT_USER
    USING (true)
    WITH CHECK (true);

CREATE POLICY bartzeit_backend_access ON customers
    TO CURRENT_USER
    USING (true)
    WITH CHECK (true);

CREATE POLICY bartzeit_backend_access ON appointments
    TO CURRENT_USER
    USING (true)
    WITH CHECK (true);
