import axios from 'axios';

export interface ApiError {
  status: number | null;
  code: string | null;
  message: string;
}

/** The backend answers every error with `{message, code}`; callers must branch on `code`, never on `message`. */
export function toApiError(error: unknown): ApiError {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: unknown; code?: unknown } | undefined;
    return {
      status: error.response?.status ?? null,
      code: typeof data?.code === 'string' ? data.code : null,
      message: typeof data?.message === 'string' ? data.message : error.message,
    };
  }
  return { status: null, code: null, message: error instanceof Error ? error.message : 'Unexpected error' };
}
