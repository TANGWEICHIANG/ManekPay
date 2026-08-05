import { Navigate, Route, Routes } from 'react-router-dom';
import { LoginPage } from './components/pages/LoginPage';
import { RegisterPage } from './components/pages/RegisterPage';
import { DashboardPage } from './components/pages/DashboardPage';
import { AppLayout } from './components/templates/AppLayout';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { ROUTES } from './constants/routes';

function App() {
  return (
    <Routes>
      <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      <Route path={ROUTES.REGISTER} element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to={ROUTES.LOGIN} replace />} />
    </Routes>
  );
}

export default App;
