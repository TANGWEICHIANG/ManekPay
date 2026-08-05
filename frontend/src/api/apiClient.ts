import { useAuthStore } from '../store/authStore';
import { API_PATHS } from '../constants/paths';
import type { TokenResponse } from '../store/models/auth.model';

const BASE_URL = `${import.meta.env.VITE_API_URL || ''}/api`;

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  isFormData?: boolean;
}

async function rawRequest(path: string, options: RequestOptions, accessToken: string | null): Promise<Response> {
  const headers: Record<string, string> = {};
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }
  let body: BodyInit | undefined;
  if (options.body !== undefined) {
    if (options.isFormData) {
      body = options.body as FormData;
    } else {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(options.body);
    }
  }
  return fetch(`${BASE_URL}${path}`, {
    method: options.method || 'GET',
    headers,
    body,
  });
}

async function tryRefresh(): Promise<boolean> {
  const { refreshToken, setTokens, clearSession } = useAuthStore.getState();
  if (!refreshToken) {
    clearSession();
    return false;
  }
  const res = await fetch(`${BASE_URL}${API_PATHS.AUTH.REFRESH}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  if (!res.ok) {
    clearSession();
    return false;
  }
  const data: TokenResponse = await res.json();
  setTokens(data.accessToken, data.refreshToken);
  return true;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const accessToken = useAuthStore.getState().accessToken;
  let res = await rawRequest(path, options, accessToken);

  if (res.status === 401 && useAuthStore.getState().refreshToken) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      res = await rawRequest(path, options, useAuthStore.getState().accessToken);
    }
  }

  if (!res.ok) {
    let message = `Request failed with status ${res.status}`;
    try {
      const errorBody: { message?: string } = await res.json();
      if (errorBody.message) message = errorBody.message;
    } catch {
      // response body wasn't JSON - keep the generic message
    }
    throw new Error(message);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return res.json();
}
