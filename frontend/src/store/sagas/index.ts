import { all, fork } from 'redux-saga/effects';
import { authSaga } from './authSagas';

export default function* rootSaga() {
  yield all([
    fork(authSaga),
  ]);
}
