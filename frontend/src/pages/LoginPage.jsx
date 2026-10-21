import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();

  const resetSuccess = searchParams.get('resetSuccess');
  const logoutParam = searchParams.get('logout');

  const handleSubmit = (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    // Stage 1 authentication: Route to MFA if configured or directly log in
    setTimeout(() => {
      setLoading(false);
      if (!email || !password) {
        setError('Please enter both Email and Password.');
        return;
      }

      // Check for Admin, Officer, Agent, or Customer persona
      let role = 'ROLE_CUSTOMER';
      let name = 'Rajesh Patil';
      if (email.includes('admin')) {
        role = 'ROLE_ADMIN';
        name = 'Sanjay Deshmukh (Admin)';
      } else if (email.includes('officer')) {
        role = 'ROLE_LOAN_OFFICER';
        name = 'Arun Kumar (Loan Officer)';
      } else if (email.includes('agent')) {
        role = 'ROLE_COLLECTIONS_AGENT';
        name = 'Pooja Verma (Collections)';
      } else if (email.includes('manager')) {
        role = 'ROLE_COLLECTIONS_MANAGER';
        name = 'Vikram Malhotra (Collections Mgr)';
      }

      const tempUser = { id: 101, email, name, role };
      // Stage 2: Store temporary pending auth and redirect to MFA verification
      sessionStorage.setItem('pending_mfa_user', JSON.stringify(tempUser));
      navigate('/mfa');
    }, 400);
  };

  return (
    <div className="container" style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '60px 20px' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '40px 36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <span style={{ fontSize: '38px', display: 'block', marginBottom: '8px' }}>🏦</span>
          <h2 style={{ fontFamily: 'var(--font-heading)', fontSize: '26px', fontWeight: 800, color: '#fff' }}>
            Staff & Customer Login
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '13px', marginTop: '6px' }}>
            Stage 1: Enter your Bank User ID / Email and Password
          </p>
        </div>

        {error && (
          <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.4)', color: '#fca5a5', padding: '12px', borderRadius: '10px', fontSize: '13px', marginBottom: '20px' }}>
            ⚠️ {error}
          </div>
        )}

        {logoutParam && (
          <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#6ee7b7', padding: '12px', borderRadius: '10px', fontSize: '13px', marginBottom: '20px' }}>
            ✓ You have been logged out securely.
          </div>
        )}

        {resetSuccess && (
          <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.4)', color: '#6ee7b7', padding: '12px', borderRadius: '10px', fontSize: '13px', marginBottom: '20px' }}>
            ✓ Password has been reset successfully. Please log in with your new password.
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '18px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
              User ID / Email Address
            </label>
            <input 
              type="text" 
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. customer@bank.com or admin@bank.com"
              required
              style={{ width: '100%', padding: '12px 16px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '10px', color: '#fff', fontSize: '14px', outline: 'none' }}
            />
          </div>

          <div style={{ marginBottom: '12px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
              Password
            </label>
            <input 
              type="password" 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your security password"
              required
              style={{ width: '100%', padding: '12px 16px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '10px', color: '#fff', fontSize: '14px', outline: 'none' }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '24px' }}>
            <Link to="/forgot-password" style={{ fontSize: '13px', color: '#f59e0b', textDecoration: 'none', fontWeight: 600 }}>
              Forgot Password?
            </Link>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="btn-primary" 
            style={{ width: '100%', padding: '13px' }}
          >
            {loading ? 'Authenticating...' : 'Proceed to Verification ➔'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '13px', color: '#94a3b8' }}>
          Customer without an account?{' '}
          <Link to="/register" style={{ color: '#60a5fa', fontWeight: 700, textDecoration: 'none' }}>
            Sign Up Now
          </Link>
        </div>

        {/* Demo Quick Logins */}
        <div style={{ marginTop: '24px', paddingTop: '20px', borderTop: '1px solid rgba(255,255,255,0.08)' }}>
          <div style={{ fontSize: '11px', color: '#64748b', textTransform: 'uppercase', fontWeight: 700, marginBottom: '10px', textAlign: 'center' }}>
            Quick Demo Auto-Fill:
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
            <button 
              type="button" 
              onClick={() => { setEmail('rajesh.patil@bank.com'); setPassword('CustomerPass123!'); }}
              style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', color: '#94a3b8', padding: '6px', borderRadius: '6px', fontSize: '11px', cursor: 'pointer' }}
            >
              👤 Customer
            </button>
            <button 
              type="button" 
              onClick={() => { setEmail('admin@bank.com'); setPassword('Admin@Bank123!'); }}
              style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', color: '#94a3b8', padding: '6px', borderRadius: '6px', fontSize: '11px', cursor: 'pointer' }}
            >
              🛡️ Bank Admin
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
