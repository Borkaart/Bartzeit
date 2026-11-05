import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./client";
import { ApiError, getFriendlyErrorMessage } from "./errors";

describe("API error handling", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it.each([400, 404, 409])("preserves HTTP %i and the backend message", async (status) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status, code: "API_ERROR", message: `Failure ${status}` }),
      { status, headers: { "Content-Type": "application/json" } },
    )));

    await expect(apiRequest("/test")).rejects.toMatchObject({
      status,
      message: `Failure ${status}`,
    });
  });

  it("uses a readable error for non-JSON responses", async () => {
    const error = await ApiError.fromResponse(new Response("not json", { status: 503 }));
    expect(error.status).toBe(503);
    expect(error.message).toContain("503");
  });

  it("explains an unavailable API without exposing request data", () => {
    expect(getFriendlyErrorMessage(new TypeError("fetch failed"))).toContain("backend");
  });

  it.each([
    [400, "backend message"],
    [404, "Não encontramos"],
    [409, "conflito"],
  ])("maps HTTP %i to an actionable message", (status, expected) => {
    expect(getFriendlyErrorMessage(new ApiError("backend message", status))).toContain(expected);
  });
});
