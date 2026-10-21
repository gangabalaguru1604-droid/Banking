import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../context/AuthContext';
import { Lock, Mail, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();
  const { refreshAuth } = useAuth();

  const successMessage = (location.state as any)?.message;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please enter both your registered email and password.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const res = await authApi.login({ email: email.trim(), password });
      if (res.success) {
        if (res.mfaRequired) {
          if (res.flow === 'mfa-enroll') {
            navigate('/mfa-enroll', { replace: true });
          } else {
            navigate('/mfa-verify', { replace: true });
          }
        } else {
          await refreshAuth();
          navigate('/dashboard', { replace: true });
        }
      } else {
        setError(res.error || 'Invalid credentials.');
      }
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || 'Login failed. Please check your credentials.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: '28px' }}>
        <div style={{
          width: '52px',
          height: '52px',
          borderRadius: '50%',
          backgroundColor: '#EFF6FF',
          color: '#0050A4',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 12px'
        }}>
          <Lock size={24} />
        </div>
        <h2 style={{ fontSize: '24px', fontWeight: 800, color: '#0A2E65', marginBottom: '6px' }}>
          Net Banking Login
        </h2>
        <p style={{ fontSize: '13px', color: '#64748B' }}>
          Secure access to Bank of India Smart Loan Management
        </p>
      </div>

      {successMessage && (
        <div style={{
          backgroundColor: '#F0FDF4',
          border: '1px solid #BBF7D0',
          color: '#166534',
          padding: '12px 14px',
          borderRadius: '8px',
          fontSize: '13px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <ShieldCheck size={18} />
          <span>{successMessage}</span>
        </div>
      )}

      {error && (
        <div style={{
          backgroundColor: '#FEF2F2',
          border: '1px solid #FEE2E2',
          color: '#991B1B',
          padding: '12px 14px',
          borderRadius: '8px',
          fontSize: '13px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'flex-start',
          gap: '8px'
        }}>
          <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
            Registered Email / Customer ID
          </label>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
              <Mail size={18} />
            </div>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. rahul.sharma@example.com"
              required
              autoFocus
              style={{
                width: '100%',
                padding: '10px 14px 10px 40px',
                borderRadius: '6px',
                border: '1px solid #CBD5E1',
                fontSize: '14px',
                color: '#0A2E65',
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>
        </div>

        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
            <label style={{ fontSize: '13px', fontWeight: 700, color: '#0A2E65' }}>
              Password
            </label>
            <Link
              to="/forgot-password"
              style={{ fontSize: '12px', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}
            >
              Forgot Password?
            </Link>
          </div>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
              <Lock size={18} />
            </div>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your confidential password"
              required
              style={{
                width: '100%',
                padding: '10px 14px 10px 40px',
                borderRadius: '6px',
                border: '1px solid #CBD5E1',
                fontSize: '14px',
                color: '#0A2E65',
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{
            padding: '12px 20px',
            backgroundColor: '#0050A4',
            color: '#FFFFFF',
            border: 'none',
            borderRadius: '6px',
            fontSize: '15px',
            fontWeight: 700,
            cursor: loading ? 'not-allowed' : 'pointer',
            opacity: loading ? 0.8 : 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            boxShadow: '0 4px 12px rgba(0, 80, 164, 0.25)',
            marginTop: '8px'
          }}
        >
          {loading ? 'Authenticating...' : 'Sign In Securely'}
          {!loading && <ArrowRight size={18} />}
        </button>
      </form>

      <div style={{
        marginTop: '24px',
        paddingTop: '20px',
        borderTop: '1px solid #E2E8F0',
        textAlign: 'center',
        fontSize: '13px',
        color: '#64748B'
      }}>
        Don't have a Bank of India loan account?{' '}
        <Link to="/register" style={{ color: '#0050A4', fontWeight: 700, textDecoration: 'none' }}>
          Register Now
        </Link>
      </div>
    </div>
  );
};

export default LoginPage;
