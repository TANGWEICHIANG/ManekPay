import { type FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../templates/AuthLayout';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { useLogin } from '../../hooks/useAuth';
import { ROUTES } from '../../constants/routes';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();
  const login = useLogin();

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
        {login.isError && (
          <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
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
