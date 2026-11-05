import { ApiError } from "./errors";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080").replace(/\/+$/, "");
const REQUEST_TIMEOUT_MS = 10_000;

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const headers = new Headers(init?.headers);
    headers.set("Accept", "application/json");
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers,
      signal: controller.signal,
    });

    if (!response.ok) {
      throw await ApiError.fromResponse(response);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const contentType = response.headers.get("content-type") ?? "";
    if (!contentType.includes("application/json")) {
      throw new ApiError("A API retornou uma resposta em formato inesperado.", response.status);
    }

    try {
      return (await response.json()) as T;
    } catch {
      throw new ApiError("A API retornou um JSON inválido.", response.status);
    }
  } finally {
    window.clearTimeout(timeout);
  }
}
