import { apiRequest } from "./client";
import type { Availability, Barbershop, Service } from "./types";

const tenantPath = (slug: string) => encodeURIComponent(slug);

export function listBarbershops(page = 0, size = 20): Promise<Barbershop[]> {
  return apiRequest(`/api/public/barbershops?page=${page}&size=${size}`);
}

export function getBarbershop(slug: string): Promise<Barbershop> {
  return apiRequest(`/api/public/barbershops/${tenantPath(slug)}`);
}

export function listServices(slug: string): Promise<Service[]> {
  return apiRequest(`/api/public/barbershops/${tenantPath(slug)}/services`);
}

export function getAvailability(slug: string, date: string, serviceId: string): Promise<Availability[]> {
  const query = new URLSearchParams({ date, serviceId });
  return apiRequest(`/api/v1/barbershops/${tenantPath(slug)}/availability?${query}`);
}
