import React, { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { Lock, AlertCircle, ArrowRight, CheckCircle2 } from 'lucide-react';

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) {
      setError('Invalid or missing password reset token. Please request a new link.');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match. Please re-enter.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const res = await authApi.resetPassword({
        token,
        password,
        confirmPassword
      });

      if (res.success) {
        navigate('/login', {
          replace: true,
          state: { message: res.message || 'Password has been reset successfully. Please log in.' }
        });
      } else {
        setError(res.error || 'Failed to reset password.');
      }
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || 'Reset password link is expired or invalid.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: '24px' }}>
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
        <h2 style={{ fontSize: '22px', fontWeight: 800, color: '#0A2E65', marginBottom: '6px' }}>
          Set New Password
        </h2>
        <p style={{ fontSize: '13px', color: '#64748B' }}>
          Create a new strong, confidential password for your account
        </p>
      </div>

      {!token && (
        <div style={{
          backgroundColor: '#FEF2F2',
          border: '1px solid #FEE2E2',
          color: '#991B1B',
          padding: '12px 14px',
          borderRadius: '8px',
          fontSize: '13px',
          marginBottom: '20px'
        }}>
          No password reset token found in URL. Please use the exact link sent to your email.
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

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
            New Password
          </label>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
              <Lock size={18} />
            </div>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Minimum 8 characters"
              required
              disabled={!token}
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
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
            Confirm New Password
          </label>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
              <Lock size={18} />
            </div>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Re-enter new password"
              required
              disabled={!token}
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
          disabled={loading || !token}
          style={{
            padding: '12px 20px',
            backgroundColor: '#0050A4',
            color: '#FFFFFF',
            border: 'none',
            borderRadius: '6px',
            fontSize: '15px',
            fontWeight: 700,
            cursor: loading || !token ? 'not-allowed' : 'pointer',
            opacity: loading || !token ? 0.7 : 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            boxShadow: '0 4px 12px rgba(0, 80, 164, 0.25)',
            marginTop: '8px'
          }}
        >
          {loading ? 'Updating Password...' : 'Save New Password'}
          {!loading && <ArrowRight size={18} />}
        </button>
      </form>

      <div style={{
        marginTop: '24px',
        paddingTop: '16px',
        borderTop: '1px solid #E2E8F0',
        textAlign: 'center',
        fontSize: '13px'
      }}>
        <Link to="/login" style={{ color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
          ← Cancel and Return to Login
        </Link>
      </div>
    </div>
  );
};

export default ResetPasswordPage;
