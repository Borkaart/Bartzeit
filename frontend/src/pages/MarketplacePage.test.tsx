import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listBarbershops } from "../api/bartzeit";
import { MarketplacePage } from "./MarketplacePage";

vi.mock("../api/bartzeit", () => ({
  listBarbershops: vi.fn(),
}));

const mockedList = vi.mocked(listBarbershops);

function renderMarketplace() {
  return render(
    <MemoryRouter>
      <MarketplacePage />
    </MemoryRouter>,
  );
}

describe("marketplace states", () => {
  beforeEach(() => {
    mockedList.mockReset();
  });

  it("shows loading and then the API barbershop list", async () => {
    let resolveList!: (shops: { id: string; slug: string; name: string; city: string; address: string; phone: null; active: boolean }[]) => void;
    mockedList.mockReturnValue(new Promise((resolve) => {
      resolveList = resolve;
    }));

    renderMarketplace();
    expect(screen.getByRole("status")).toHaveTextContent("Buscando barbearias");
    resolveList([{
      id: "1",
      slug: "central",
      name: "Barbearia Central",
      city: "Curitiba",
      address: "Rua Um, 10",
      phone: null,
      active: true,
    }]);

    expect(await screen.findByText("Barbearia Central")).toBeInTheDocument();
    expect(screen.getByText("Curitiba")).toBeInTheDocument();
  });

  it("shows the empty state when the API returns no shops", async () => {
    mockedList.mockResolvedValue([]);
    renderMarketplace();

    expect(await screen.findByText(/Não há barbearias cadastradas/)).toBeInTheDocument();
  });

  it("shows an error and retries the request", async () => {
    mockedList.mockRejectedValueOnce(new TypeError("unavailable")).mockResolvedValueOnce([]);
    renderMarketplace();

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível conectar à API");
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));

    await waitFor(() => expect(mockedList).toHaveBeenCalledTimes(2));
    expect(await screen.findByText(/Não há barbearias cadastradas/)).toBeInTheDocument();
  });
});
