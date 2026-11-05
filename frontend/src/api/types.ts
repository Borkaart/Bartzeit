export interface Barbershop {
  id: string;
  name: string;
  slug: string;
  city: string;
  address: string;
  phone: string | null;
  active: boolean;
}

export interface Service {
  id: string;
  barbershopId: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  active: boolean;
}

export interface Availability {
  barberId: string;
  barberName: string;
  available: boolean;
  slots: string[];
}

export interface ApiErrorPayload {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  path?: string;
}
