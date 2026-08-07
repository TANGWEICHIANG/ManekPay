import { type FormEvent, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../templates/AuthLayout';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { useLogin } from '../../hooks/useAuth';
import { ROUTES } from '../../constants/routes';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const login = useLogin();
  const justRegistered = (location.state as { registered?: boolean } | null)?.registered;

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    login.mutate(
      { email, password },
      { onSuccess: () => navigate(ROUTES.DASHBOARD) }
    );
  };

  return (
    <AuthLayout title="Log in to ManekPay">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {justRegistered && !login.isError && (
          <p role="status" className="rounded-md bg-success/10 px-3 py-2 text-sm text-success">
            Account created — log in to continue.
          </p>
        )}
        {login.isError && (
          <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
            {login.error instanceof Error ? login.error.message : 'Login failed'}
          </p>
        )}
        <Input
          label="Email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <Input
          label="Password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Button type="submit" isLoading={login.isPending}>
          Log in
        </Button>
        <p className="text-center text-sm text-foreground">
          No account?{' '}
          <Link to={ROUTES.REGISTER} className="text-primary underline">
            Register
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}
