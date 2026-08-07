import { type FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../templates/AuthLayout';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { useRegister } from '../../hooks/useAuth';
import { ROUTES } from '../../constants/routes';

export function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const navigate = useNavigate();
  const register = useRegister();

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    register.mutate(
      { email, password, fullName },
      { onSuccess: () => navigate(ROUTES.LOGIN, { state: { registered: true } }) }
    );
  };

  return (
    <AuthLayout title="Create your ManekPay account">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {register.isError && (
          <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
            {register.error instanceof Error ? register.error.message : 'Registration failed'}
          </p>
        )}
        <Input
          label="Full name"
          required
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
        />
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
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Button type="submit" isLoading={register.isPending}>
          Register
        </Button>
        <p className="text-center text-sm text-foreground">
          Already have an account?{' '}
          <Link to={ROUTES.LOGIN} className="text-primary underline">
            Log in
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}
