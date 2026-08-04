import { LoginRequest, UserProfile, TokenResponse } from '../models/auth.model';

export const AuthActionTypes = {
  LOGIN_REQUEST: 'AUTH/LOGIN_REQUEST',
  LOGIN_SUCCESS: 'AUTH/LOGIN_SUCCESS',
  LOGIN_FAILURE: 'AUTH/LOGIN_FAILURE',
  LOGOUT: 'AUTH/LOGOUT',
  SET_USER: 'AUTH/SET_USER',
} as const;

export const loginRequest = (payload: LoginRequest) => ({
  type: AuthActionTypes.LOGIN_REQUEST,
  payload,
});

export const loginSuccess = (payload: { user: UserProfile; tokens: TokenResponse }) => ({
  type: AuthActionTypes.LOGIN_SUCCESS,
  payload,
});

export const loginFailure = (error: string) => ({
  type: AuthActionTypes.LOGIN_FAILURE,
  payload: error,
});

export const logout = () => ({
  type: AuthActionTypes.LOGOUT,
});
