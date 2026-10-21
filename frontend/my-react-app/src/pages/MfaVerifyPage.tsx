import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, AlertCircle, ArrowRight, KeyRound } from 'lucide-react';

export const MfaVerifyPage: React.FC = () => {
  const [code, setCode] = useState('');
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [attemptsRemaining, setAttemptsRemaining] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);

  const navigate = useNavigate();
  const { refreshAuth } = useAuth();

  useEffect(() => {
    let isMounted = true;
    async function loadMfaData() {
      try {
        const data = await authApi.getMfaVerifyData();
        if (isMounted) {
          if (data.error) {
            navigate('/login', { replace: true, state: { error: data.error } });
          } else {
            setEmail(data.accountEmail || '');
          }
        }
      } catch (err: any) {
        if (isMounted) {
          navigate('/login', { replace: true, state: { error: 'Please log in to continue.' } });
        }
      } finally {
        if (isMounted) setInitialLoading(false);
      }
    }
    loadMfaData();
    return () => { isMounted = false; };
  }, [navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanCode = code.replace(/\D/g, '');
    if (cleanCode.length !== 6) {
      setError('Please enter the full 6-digit verification code.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const res = await authApi.verifyMfa(cleanCode);
      if (res.success) {
        await refreshAuth();
        navigate('/dashboard', { replace: true });
      } else {
        setError(res.error || 'Verification failed.');
        if (res.attemptsRemaining !== undefined) {
          setAttemptsRemaining(res.attemptsRemaining);
        }
      }
    } catch (err: any) {
      const data = err.response?.data;
      const msg = data?.error || err.message || 'Verification failed.';
      setError(msg);
      if (data?.attemptsRemaining !== undefined) {
        setAttemptsRemaining(data.attemptsRemaining);
      }
      if (err.response?.status === 401 && msg.includes('locked')) {
        setTimeout(() => {
          navigate('/login', { replace: true, state: { error: msg } });
        }, 2000);
      }
    } finally {
      setLoading(false);
    }
  };

  if (initialLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '40px 0' }}>
        <div style={{
          width: '36px',
          height: '36px',
          border: '3px solid #E2E8F0',
          borderTopColor: '#0050A4',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite',
          margin: '0 auto 12px'
        }} />
        <p style={{ color: '#64748B', fontSize: '13px' }}>Verifying 2FA session...</p>
      </div>
    );
  }

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: '24px' }}>
        <div style={{
          width: '56px',
          height: '56px',
          borderRadius: '50%',
          backgroundColor: '#EFF6FF',
          color: '#0050A4',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 12px'
        }}>
          <KeyRound size={26} />
        </div>
        <h2 style={{ fontSize: '22px', fontWeight: 800, color: '#0A2E65', marginBottom: '6px' }}>
          Two-Factor Authentication
        </h2>
        <p style={{ fontSize: '13px', color: '#64748B' }}>
          Enter the 6-digit code from your Google Authenticator or TOTP app for:
        </p>
        {email && (
          <div style={{
            fontSize: '13px',
            fontWeight: 700,
            color: '#0050A4',
            marginTop: '4px',
            backgroundColor: '#F1F5F9',
            padding: '4px 10px',
            borderRadius: '4px',
            display: 'inline-block'
          }}>
            {email}
          </div>
        )}
      </div>

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
          <div>
            <div>{error}</div>
            {attemptsRemaining !== null && (
              <div style={{ fontSize: '12px', fontWeight: 600, marginTop: '4px', color: '#B91C1C' }}>
                Remaining attempts before account lock: {attemptsRemaining}
              </div>
            )}
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '8px', textAlign: 'center' }}>
            6-Digit Security Code
          </label>
          <input
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
            placeholder="• • • • • •"
            autoFocus
            required
            style={{
              width: '100%',
              padding: '12px',
              fontSize: '28px',
              fontWeight: 800,
              letterSpacing: '12px',
              textAlign: 'center',
              borderRadius: '8px',
              border: '2px solid #CBD5E1',
              color: '#0A2E65',
              backgroundColor: '#F8FAFC',
              outline: 'none',
              boxSizing: 'border-box'
            }}
          />
        </div>

        <button
          type="submit"
          disabled={loading || code.length !== 6}
          style={{
            padding: '12px 20px',
            backgroundColor: '#0050A4',
            color: '#FFFFFF',
            border: 'none',
            borderRadius: '6px',
            fontSize: '15px',
            fontWeight: 700,
            cursor: loading || code.length !== 6 ? 'not-allowed' : 'pointer',
            opacity: loading || code.length !== 6 ? 0.7 : 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            boxShadow: '0 4px 12px rgba(0, 80, 164, 0.25)'
          }}
        >
          {loading ? 'Verifying Code...' : 'Confirm & Authenticate'}
          {!loading && <ArrowRight size={18} />}
        </button>
      </form>

      <div style={{
        marginTop: '24px',
        paddingTop: '16px',
        borderTop: '1px solid #E2E8F0',
        textAlign: 'center',
        fontSize: '12px',
        color: '#64748B'
      }}>
        Lost access to your authenticator app? Contact Bank of India Help Desk at 1800-220-229.
      </div>
    </div>
  );
};

export default MfaVerifyPage;
