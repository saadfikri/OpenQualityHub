import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { authApi } from '@/api/auth';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const response = await authApi.login({ email, password });
      login(response);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = () => {
    setEmail('sarah.chen@demo.oqh');
    setPassword('Demo123!');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[var(--oqh-page)]">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-sm border border-[var(--oqh-border)] p-8">
          <div className="text-center mb-8">
            <img src="/logo.svg" alt="" className="h-20 mx-auto" />
            <h1 className="sr-only">Open Quality Hub</h1>
            <p className="text-xs text-[var(--oqh-text-muted)] mt-1">Quality Engineering Platform</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg border border-red-200" role="alert" aria-live="polite">
                {error}
              </div>
            )}

            <Input
              id="login-email"
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoFocus
              autoComplete="email"
            />

            <Input
              id="login-password"
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />

            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Signing in...' : 'Sign in'}
            </Button>
          </form>

          <div className="mt-6 pt-6 border-t border-[var(--oqh-border)]">
            <p className="text-xs text-[var(--oqh-text-muted)] text-center mb-3">Demo credentials</p>
            <button
              type="button"
              onClick={handleDemoLogin}
              className="w-full text-sm text-[var(--oqh-blue)] hover:text-[var(--oqh-primary)] font-medium py-2 px-4 rounded-lg hover:bg-[var(--oqh-active-bg)] transition-colors"
            >
              Fill demo login (sarah.chen@demo.oqh)
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
