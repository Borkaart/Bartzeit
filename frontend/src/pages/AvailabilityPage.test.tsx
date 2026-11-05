import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getAvailability, getBarbershop, listServices } from "../api/bartzeit";
import { AvailabilityPage } from "./AvailabilityPage";

vi.mock("../api/bartzeit", () => ({
  getAvailability: vi.fn(),
  getBarbershop: vi.fn(),
  listServices: vi.fn(),
}));

const mockedGetShop = vi.mocked(getBarbershop);
const mockedListServices = vi.mocked(listServices);
const mockedAvailability = vi.mocked(getAvailability);

describe("availability consultation", () => {
  beforeEach(() => {
    mockedGetShop.mockReset().mockResolvedValue({
      id: "shop-1",
      name: "Barbearia Central",
      slug: "central",
      city: "Curitiba",
      address: "Rua Um, 10",
      phone: null,
      active: true,
    });
    mockedListServices.mockReset().mockResolvedValue([{
      id: "service-1",
      barbershopId: "shop-1",
      name: "Corte clássico",
      description: null,
      durationMinutes: 45,
      price: 55,
      active: true,
    }]);
    mockedAvailability.mockReset().mockResolvedValue([{
      barberId: "barber-1",
      barberName: "Rafael",
      available: true,
      slots: ["2026-11-06T09:00:00"],
    }]);
  });

  it("shows API availability and never claims the slot was booked", async () => {
    render(
      <MemoryRouter initialEntries={["/barbearias/central/agendar?serviceId=service-1"]}>
        <Routes>
          <Route path="/barbearias/:slug/agendar" element={<AvailabilityPage />} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole("button", { name: "Consultar disponibilidade" }));
    expect(await screen.findByRole("heading", { name: "Rafael" })).toBeInTheDocument();
    expect(mockedAvailability).toHaveBeenCalledWith("central", expect.any(String), "service-1");

    fireEvent.click(screen.getByRole("button", { name: "09:00" }));
    expect(screen.getByRole("button", { name: "09:00" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByText(/Nenhum horário foi reservado/)).toBeInTheDocument();
  });
});
