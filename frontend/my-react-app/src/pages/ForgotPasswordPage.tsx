import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { KeyRound, Mail, AlertCircle, CheckCircle2, ArrowRight } from 'lucide-react';

export const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setError('Please provide your registered email address.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const res = await authApi.forgotPassword(email.trim());
      setSubmitted(true);
      setMessage(res.message || 'If the email matches an active account, a password reset link has been dispatched.');
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || 'Failed to request password reset.';
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
          <KeyRound size={24} />
        </div>
        <h2 style={{ fontSize: '22px', fontWeight: 800, color: '#0A2E65', marginBottom: '6px' }}>
          Reset Account Password
        </h2>
        <p style={{ fontSize: '13px', color: '#64748B' }}>
          Enter your registered email to receive a secure time-limited password recovery link
        </p>
      </div>

      {submitted ? (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <div style={{
            backgroundColor: '#F0FDF4',
            border: '1px solid #BBF7D0',
            color: '#166534',
            padding: '16px',
            borderRadius: '8px',
            fontSize: '13px',
            marginBottom: '24px',
            lineHeight: '1.5'
          }}>
            <CheckCircle2 size={24} style={{ color: '#16A34A', margin: '0 auto 8px', display: 'block' }} />
            {message}
          </div>
          <Link
            to="/login"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '10px 20px',
              backgroundColor: '#0050A4',
              color: '#FFFFFF',
              borderRadius: '6px',
              textDecoration: 'none',
              fontWeight: 600,
              fontSize: '14px'
            }}
          >
            Back to Login
          </Link>
        </div>
      ) : (
        <>
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
                Account Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
                  <Mail size={18} />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
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
                boxShadow: '0 4px 12px rgba(0, 80, 164, 0.25)'
              }}
            >
              {loading ? 'Submitting Request...' : 'Send Recovery Link'}
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
              ← Return to Net Banking Login
            </Link>
          </div>
        </>
      )}
    </div>
  );
};

export default ForgotPasswordPage;
