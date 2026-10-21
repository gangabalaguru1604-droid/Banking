import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../context/AuthContext';
import { QrCode, Copy, Check, Clock, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';

export const MfaEnrollPage: React.FC = () => {
  const [qrCodeDataUri, setQrCodeDataUri] = useState<string>('');
  const [secretKey, setSecretKey] = useState<string>('');
  const [rawSecret, setRawSecret] = useState<string>('');
  const [accountEmail, setAccountEmail] = useState<string>('');
  const [secondsRemaining, setSecondsRemaining] = useState<number>(300);
  const [attemptsRemaining, setAttemptsRemaining] = useState<number>(3);
  const [code, setCode] = useState<string>('');
  const [copied, setCopied] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [initialLoading, setInitialLoading] = useState<boolean>(true);

  const navigate = useNavigate();
  const { refreshAuth } = useAuth();

  useEffect(() => {
    let isMounted = true;
    async function loadEnrollData() {
      try {
        const data = await authApi.getMfaEnrollData();
        if (isMounted) {
          if (data.error) {
            navigate('/login', { replace: true, state: { error: data.error } });
          } else {
            setQrCodeDataUri(data.qrCodeDataUri || '');
            setSecretKey(data.secretKey || '');
            setRawSecret(data.rawSecret || '');
            setAccountEmail(data.accountEmail || '');
            setSecondsRemaining(data.secondsRemaining || 300);
            if (data.attemptsRemaining !== undefined) {
              setAttemptsRemaining(data.attemptsRemaining);
            }
          }
        }
      } catch (err: any) {
        if (isMounted) {
          navigate('/login', { replace: true, state: { error: 'Please log in to initiate MFA enrollment.' } });
        }
      } finally {
        if (isMounted) setInitialLoading(false);
      }
    }

    loadEnrollData();
    return () => { isMounted = false; };
  }, [navigate]);

  // Enrollment countdown timer
  useEffect(() => {
    if (secondsRemaining <= 0) {
      navigate('/login', { replace: true, state: { error: 'MFA enrollment window expired. Please log in again.' } });
      return;
    }

    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          navigate('/login', { replace: true, state: { error: 'MFA enrollment window expired. Please log in again.' } });
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [secondsRemaining, navigate]);

  const handleCopySecret = () => {
    const textToCopy = rawSecret || secretKey.replace(/\s+/g, '');
    navigator.clipboard.writeText(textToCopy);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanCode = code.replace(/\D/g, '');
    if (cleanCode.length !== 6) {
      setError('Please enter the full 6-digit confirmation code from your app.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const res = await authApi.verifyMfaEnroll(cleanCode);
      if (res.success) {
        await refreshAuth();
        navigate('/dashboard', { replace: true });
      } else {
        setError(res.error || 'Invalid authentication code.');
        if (res.attemptsRemaining !== undefined) {
          setAttemptsRemaining(res.attemptsRemaining);
        }
      }
    } catch (err: any) {
      const data = err.response?.data;
      const msg = data?.error || err.message || 'MFA enrollment verification failed.';
      setError(msg);
      if (data?.attemptsRemaining !== undefined) {
        setAttemptsRemaining(data.attemptsRemaining);
      }
      if (err.response?.status === 401 && msg.includes('exceeded')) {
        setTimeout(() => {
          navigate('/login', { replace: true, state: { error: msg } });
        }, 2000);
      }
    } finally {
      setLoading(false);
    }
  };

  const formatMinutes = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
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
        <p style={{ color: '#64748B', fontSize: '13px' }}>Generating TOTP cryptographic credentials...</p>
      </div>
    );
  }

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: '20px' }}>
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
          <QrCode size={26} />
        </div>
        <h2 style={{ fontSize: '22px', fontWeight: 800, color: '#0A2E65', marginBottom: '6px' }}>
          Enroll Two-Factor Authentication
        </h2>
        <p style={{ fontSize: '13px', color: '#64748B' }}>
          Mandatory RBI security compliance for Bank of India Loan Management
        </p>
      </div>

      {/* Countdown Timer Banner */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '10px 14px',
        backgroundColor: '#FFF7ED',
        border: '1px solid #FED7AA',
        borderRadius: '8px',
        marginBottom: '20px',
        fontSize: '13px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#C2410C', fontWeight: 600 }}>
          <Clock size={16} />
          <span>Window expires in: <strong>{formatMinutes(secondsRemaining)}</strong></span>
        </div>
        <div style={{ color: '#9A3412', fontWeight: 700 }}>
          {attemptsRemaining} {attemptsRemaining === 1 ? 'attempt' : 'attempts'} left
        </div>
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
          <span>{error}</span>
        </div>
      )}

      {/* Step 1: Scan QR Code */}
      <div style={{
        textAlign: 'center',
        backgroundColor: '#F8FAFC',
        border: '1px solid #E2E8F0',
        borderRadius: '8px',
        padding: '20px',
        marginBottom: '20px'
      }}>
        <div style={{ fontSize: '12px', fontWeight: 700, color: '#64748B', textTransform: 'uppercase', marginBottom: '12px' }}>
          Step 1: Scan with Google Authenticator or Microsoft Authenticator
        </div>

        {qrCodeDataUri ? (
          <img
            src={qrCodeDataUri}
            alt="TOTP QR Code"
            style={{
              width: '180px',
              height: '180px',
              margin: '0 auto',
              display: 'block',
              borderRadius: '6px',
              border: '2px solid #CBD5E1',
              backgroundColor: '#FFFFFF',
              padding: '8px'
            }}
          />
        ) : (
          <div style={{ height: '180px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <span>QR Code generation...</span>
          </div>
        )}

        <div style={{ marginTop: '14px' }}>
          <div style={{ fontSize: '12px', color: '#64748B', marginBottom: '6px' }}>
            Can't scan? Enter secret key manually:
          </div>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            backgroundColor: '#FFFFFF',
            border: '1px solid #CBD5E1',
            borderRadius: '6px',
            padding: '6px 12px'
          }}>
            <code style={{ fontSize: '13px', fontWeight: 700, color: '#0A2E65', letterSpacing: '1px' }}>
              {secretKey || rawSecret}
            </code>
            <button
              type="button"
              onClick={handleCopySecret}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                color: copied ? '#16A34A' : '#0050A4',
                padding: '4px',
                display: 'flex',
                alignItems: 'center'
              }}
              title="Copy secret key"
            >
              {copied ? <Check size={16} /> : <Copy size={16} />}
            </button>
          </div>
        </div>
      </div>

      {/* Step 2: Confirmation Code Form */}
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px', textAlign: 'center' }}>
            Step 2: Enter 6-digit code shown in your app
          </label>
          <input
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
            placeholder="• • • • • •"
            required
            style={{
              width: '100%',
              padding: '12px',
              fontSize: '26px',
              fontWeight: 800,
              letterSpacing: '10px',
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
            backgroundColor: '#16A34A',
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
            boxShadow: '0 4px 12px rgba(22, 163, 74, 0.25)'
          }}
        >
          {loading ? 'Activating 2FA...' : 'Complete Enrollment & Enter'}
          {!loading && <ArrowRight size={18} />}
        </button>
      </form>
    </div>
  );
};

export default MfaEnrollPage;
