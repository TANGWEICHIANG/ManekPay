import { AuthActionTypes } from '../actions/authActions';
import { UserProfile } from '../models/auth.model';
import { STORAGE_KEYS } from '../../constants/localStorageKeys';

export interface AuthState {
  user: UserProfile | null;
  token: string | null;
  isLoading: boolean;
  error: string | null;
}

const initialState: AuthState = {
  user: null,
  token: localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN),
  isLoading: false,
  error: null,
};

export const authReducer = (state = initialState, action: any): AuthState => {
  switch (action.type) {
    case AuthActionTypes.LOGIN_REQUEST:
      return { ...state, isLoading: true, error: null };
      
    case AuthActionTypes.LOGIN_SUCCESS:
      return {
        ...state,
        isLoading: false,
        user: action.payload.user,
        token: action.payload.tokens.accessToken,
      };
      
    case AuthActionTypes.LOGIN_FAILURE:
      return { ...state, isLoading: false, error: action.payload };
      
    case AuthActionTypes.LOGOUT:
      return { ...state, user: null, token: null, error: null };
      
    default:
      return state;
  }
};
