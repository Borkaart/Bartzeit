import type { ApiErrorPayload } from "./types";

export class ApiError extends Error {
  readonly status: number | null;
  readonly code: string | null;

  constructor(message: string, status: number | null, code: string | null = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }

  static async fromResponse(response: Response): Promise<ApiError> {
    const payload = (await response.json().catch(() => null)) as ApiErrorPayload | null;

    return new ApiError(
      payload?.message || `A API respondeu com o status ${response.status}.`,
      response.status,
      payload?.code ?? null,
    );
  }
}

export function getFriendlyErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 400) return error.message || "Confira os dados enviados.";
    if (error.status === 404) return "Não encontramos esse conteúdo na plataforma.";
    if (error.status === 409) return "Este horário teve um conflito. Consulte a disponibilidade novamente.";
    if (error.status === 401 || error.status === 403) {
      return "Esta operação exige autenticação, ainda não disponível nesta versão.";
    }
    return error.message;
  }

  if (error instanceof Error && error.name === "AbortError") {
    return "A solicitação demorou mais que o esperado. Tente novamente.";
  }
  if (error instanceof TypeError) {
    return "Não foi possível conectar à API. Verifique se o backend está em execução e tente novamente.";
  }
  return "Ocorreu um erro inesperado. Tente novamente.";
}
