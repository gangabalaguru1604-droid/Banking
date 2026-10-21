import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleResetRequest = (e) => {
    e.preventDefault();
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setSubmitted(true);
    }, 500);
  };

  return (
    <div className="container" style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '60px 20px' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '40px 36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <span style={{ fontSize: '38px', display: 'block', marginBottom: '8px' }}>🔑</span>
          <h2 style={{ fontFamily: 'var(--font-heading)', fontSize: '24px', fontWeight: 800, color: '#fff' }}>
            Password Recovery
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '13px', marginTop: '6px' }}>
            Enter your registered email to receive a password reset link
          </p>
        </div>

        {submitted ? (
          <div style={{ textAlign: 'center' }}>
            <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#6ee7b7', padding: '16px', borderRadius: '12px', fontSize: '14px', marginBottom: '24px', lineHeight: 1.5 }}>
              ✓ If this email is registered with Indian Bank, a secure reset link with token has been dispatched.
            </div>
            {/* Direct testing simulation button */}
            <Link 
              to="/reset-password?token=sample_demo_token_2026"
              className="btn-primary"
              style={{ display: 'block', width: '100%', marginBottom: '16px' }}
            >
              Demo: Proceed to Reset Page ➔
            </Link>
            <Link to="/login" style={{ fontSize: '13px', color: '#94a3b8', textDecoration: 'none' }}>
              Return to Login
            </Link>
          </div>
        ) : (
          <form onSubmit={handleResetRequest}>
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
                Registered Email Address
              </label>
              <input 
                type="email" 
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="customer@bank.com"
                required
                style={{ width: '100%', padding: '12px 16px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '10px', color: '#fff', fontSize: '14px', outline: 'none' }}
              />
            </div>

            <button 
              type="submit" 
              disabled={loading}
              className="btn-primary" 
              style={{ width: '100%', padding: '13px' }}
            >
              {loading ? 'Sending Request...' : 'Send Recovery Link ➔'}
            </button>

            <div style={{ textAlign: 'center', marginTop: '24px' }}>
              <Link to="/login" style={{ fontSize: '13px', color: '#94a3b8', textDecoration: 'none' }}>
                ← Remember password? Back to Login
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
