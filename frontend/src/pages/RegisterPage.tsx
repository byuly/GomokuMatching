/**
 * RegisterPage - New user registration
 */

import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/context';
import { Button, Input, Card, CardHeader, CardTitle, CardContent } from '@/components/ui';
import { PageContainer } from '@/components/layout';
import { validateUsername, validateEmail, validatePassword } from '@/utils';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register, error, clearError } = useAuth();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setValidationErrors((prev) => ({ ...prev, [name]: '' }));
    clearError();
  };

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    // Username validation
    const usernameValidation = validateUsername(formData.username);
    if (!usernameValidation.isValid) {
      errors.username = usernameValidation.errors[0];
    }

    // Email validation
    if (!validateEmail(formData.email)) {
      errors.email = 'Invalid email address';
    }

    // Password validation
    const passwordValidation = validatePassword(formData.password);
    if (!passwordValidation.isValid) {
      errors.password = passwordValidation.errors[0];
    }

    // Confirm password
    if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      await register({
        username: formData.username,
        email: formData.email,
        password: formData.password,
      });
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
        <Card glow color="pink">
          <CardHeader>
            <CardTitle glow>New Player Registration</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              <Input
                label="Username"
                name="username"
                type="text"
                placeholder="Choose a username"
                value={formData.username}
                onChange={handleChange}
                error={validationErrors.username}
                required
                glow
              />

              <Input
                label="Email"
                name="email"
                type="email"
                placeholder="Enter your email"
                value={formData.email}
                onChange={handleChange}
                error={validationErrors.email}
                required
                glow
              />

              <Input
                label="Password"
                name="password"
                type="password"
                placeholder="Create a password"
                value={formData.password}
                onChange={handleChange}
                error={validationErrors.password}
                required
                glow
              />

              <Input
                label="Confirm Password"
                name="confirmPassword"
                type="password"
                placeholder="Confirm your password"
                value={formData.confirmPassword}
                onChange={handleChange}
                error={validationErrors.confirmPassword}
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
                variant="secondary"
                size="large"
                fullWidth
                glow
                disabled={loading}
              >
                {loading ? 'Creating...' : 'Create Account'}
              </Button>

              <div className="text-center">
                <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase mb-2">
                  Already Have an Account?
                </p>
                <Link to="/login">
                  <Button variant="primary" size="small">
                    Login
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
