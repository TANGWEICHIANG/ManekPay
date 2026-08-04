import { combineReducers } from 'redux';
import { authReducer, AuthState } from './authReducer';

// Define the global state structure
export interface RootState {
  auth: AuthState;
  // future state slices go here (e.g., kyc: KycState)
}

export const rootReducer = combineReducers({
  auth: authReducer,
});
