import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function MfaPage() {
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [countdown, setCountdown] = useState(300); // 5 mins
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          alert('MFA challenge expired. Please login again.');
          navigate('/login');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [navigate]);

  const formatCountdown = (sec) => {
    const mins = Math.floor(sec / 60);
    const s = sec % 60;
    return `${mins < 10 ? '0' : ''}${mins}:${s < 10 ? '0' : ''}${s}`;
  };

  const handleVerify = (e) => {
    e.preventDefault();
    if (code.length !== 6) {
      setError('Please enter a valid 6-digit verification code.');
      return;
    }

    setLoading(true);
    setError('');

    setTimeout(() => {
      setLoading(false);
      // Retrieve the pending user
      const savedUserStr = sessionStorage.getItem('pending_mfa_user');
      const authUser = savedUserStr ? JSON.parse(savedUserStr) : {
        id: 101,
        name: 'Rajesh Patil',
        email: 'rajesh.patil@bank.com',
        role: 'ROLE_CUSTOMER'
      };

      login(authUser);
      sessionStorage.removeItem('pending_mfa_user');
      navigate('/dashboard');
    }, 500);
  };

  return (
    <div className="container" style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '60px 20px' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '40px 36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <span style={{ fontSize: '38px', display: 'block', marginBottom: '8px' }}>🔐</span>
          <h2 style={{ fontFamily: 'var(--font-heading)', fontSize: '24px', fontWeight: 800, color: '#fff' }}>
            Two-Factor Authentication
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '13px', marginTop: '6px' }}>
            Stage 2: Enter the 6-digit code from your <strong>Authenticator App</strong>
          </p>
        </div>

        {error && (
          <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.4)', color: '#fca5a5', padding: '12px', borderRadius: '10px', fontSize: '13px', marginBottom: '20px' }}>
            ⚠️ {error}
          </div>
        )}

        <div style={{ background: 'rgba(255, 255, 255, 0.04)', padding: '12px', borderRadius: '10px', marginBottom: '24px', textAlign: 'center', fontSize: '13px', color: '#cbd5e1', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
          ⏳ Challenge expires in: <strong style={{ color: countdown < 60 ? '#f87171' : '#f59e0b', fontSize: '14px' }}>{formatCountdown(countdown)}</strong>
        </div>

        <form onSubmit={handleVerify}>
          <div style={{ marginBottom: '24px' }}>
            <input 
              type="text" 
              maxLength="6"
              pattern="[0-9]{6}"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
              placeholder="000000"
              autoFocus
              required
              style={{ width: '100%', padding: '16px', background: 'rgba(7, 11, 20, 0.8)', border: '1px solid rgba(59, 130, 246, 0.4)', borderRadius: '12px', color: '#60a5fa', fontSize: '32px', textAlign: 'center', letterSpacing: '10px', fontWeight: 800, outline: 'none' }}
            />
            <div style={{ fontSize: '11px', color: '#64748b', textAlign: 'center', marginTop: '8px' }}>
              For Demo: Enter any 6 digits (e.g. 123456)
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="btn-primary" 
            style={{ width: '100%', padding: '13px' }}
          >
            {loading ? 'Verifying...' : 'Verify & Enter Portal ➔'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '24px' }}>
          <Link to="/login" style={{ fontSize: '13px', color: '#94a3b8', textDecoration: 'none' }}>
            ← Cancel & Return to Login
          </Link>
        </div>
      </div>
    </div>
  );
}
