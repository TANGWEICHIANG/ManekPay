import { call, put, takeLatest } from 'redux-saga/effects';
import { AuthActionTypes, loginSuccess, loginFailure } from '../actions/authActions';
import { authApi } from '../../api/authApi';
import { STORAGE_KEYS } from '../../constants/localStorageKeys';
import { TokenResponse, UserProfile } from '../models/auth.model';

function* handleLogin(action: any) {
  try {
    // 1. Call the login API
    const tokens: TokenResponse = yield call(authApi.login, action.payload);
    
    // 2. Save tokens to LocalStorage
    localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, tokens.accessToken);
    localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, tokens.refreshToken);

    // 3. Fetch the user profile using the new token
    const user: UserProfile = yield call(authApi.getMe, tokens.accessToken);

    // 4. Dispatch success action to update the Redux state
    yield put(loginSuccess({ user, tokens }));
  } catch (error: any) {
    yield put(loginFailure(error.message || 'Login failed'));
  }
}

function* handleLogout() {
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
  localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
  // You could also trigger a redirect to /login here if needed
}

export function* authSaga() {
  yield takeLatest(AuthActionTypes.LOGIN_REQUEST, handleLogin);
  yield takeLatest(AuthActionTypes.LOGOUT, handleLogout);
}
