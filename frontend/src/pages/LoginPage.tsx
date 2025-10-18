/**
 * LoginPage - User authentication
 */

import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/context';
import { Button, Input, Card, CardHeader, CardTitle, CardContent } from '@/components/ui';
import { PageContainer } from '@/components/layout';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login, error, clearError } = useAuth();
  const [formData, setFormData] = useState({
    usernameOrEmail: '',
    password: '',
  });
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    clearError();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await login(formData);
      navigate('/lobby');
    } catch (err) {
      // Error is handled by AuthContext
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageContainer className="flex items-center justify-center">
      <div className="w-full max-w-md">
        <Card glow color="cyan">
          <CardHeader>
            <CardTitle glow>Player Login</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              <Input
                label="Username or Email"
                name="usernameOrEmail"
                type="text"
                placeholder="Enter username or email"
                value={formData.usernameOrEmail}
                onChange={handleChange}
                required
                glow
              />

              <Input
                label="Password"
                name="password"
                type="password"
                placeholder="Enter password"
                value={formData.password}
                onChange={handleChange}
                required
                glow
              />

              {error && (
                <div className="p-3 pixel-border border-[var(--arcade-red)] bg-[var(--arcade-black)]">
                  <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-red)]">
                    {error}
                  </p>
                </div>
              )}

              <Button
                type="submit"
                variant="primary"
                size="large"
                fullWidth
                glow
                disabled={loading}
              >
                {loading ? 'Loading...' : 'Login'}
              </Button>

              <div className="text-center">
                <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase mb-2">
                  No Account?
                </p>
                <Link to="/register">
                  <Button variant="secondary" size="small">
                    Register
                  </Button>
                </Link>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </PageContainer>
  );
};
