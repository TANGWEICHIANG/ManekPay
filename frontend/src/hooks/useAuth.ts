import { useMutation, useQuery } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import type { LoginRequest, RegisterRequest } from '../store/models/auth.model';

export function useLogin() {
  const setSession = useAuthStore((state) => state.setSession);
  return useMutation({
    mutationFn: async (credentials: LoginRequest) => {
      const tokens = await authApi.login(credentials);
      useAuthStore.getState().setTokens(tokens.accessToken, tokens.refreshToken);
      const user = await authApi.getMe();
      setSession(user, tokens.accessToken, tokens.refreshToken);
      return user;
    },
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
  });
}

export function useMe() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  return useQuery({
    queryKey: ['me'],
    queryFn: authApi.getMe,
    enabled: isAuthenticated,
  });
}

export function useLogout() {
  const clearSession = useAuthStore((state) => state.clearSession);
  return useMutation({
    mutationFn: async () => {
      const refreshToken = useAuthStore.getState().refreshToken;
      if (refreshToken) {
        await authApi.logout(refreshToken);
      }
    },
    onSettled: () => {
      clearSession();
    },
  });
}
