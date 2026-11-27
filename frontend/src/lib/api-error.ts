import { AxiosError } from "axios";

interface ErrorPayload {
  message?: string;
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ErrorPayload | undefined;
    if (data?.message && data.message.trim().length > 0) {
      return data.message;
    }
  }
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallback;
}

