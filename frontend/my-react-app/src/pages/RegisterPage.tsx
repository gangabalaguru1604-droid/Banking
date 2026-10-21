import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { UserPlus, Mail, Lock, User as UserIcon, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const validatePassword = (pass: string): string | null => {
    if (pass.length < 8) return 'Password must be at least 8 characters long.';
    if (!/[A-Z]/.test(pass)) return 'Password must contain at least one uppercase letter (A-Z).';
    if (!/[0-9]/.test(pass)) return 'Password must contain at least one numeric digit (0-9).';
    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pass)) {
      return 'Password must contain at least one special character (!@#$%^&* etc.).';
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email.trim() || !password) {
      setError('Please fill in all required fields.');
      return;
    }

    const passError = validatePassword(password);
    if (passError) {
      setError(passError);
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match. Please re-enter carefully.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const res = await authApi.register({
        name: name.trim(),
        email: email.trim().toLowerCase(),
        password
      });

      if (res.success) {
        navigate('/login', {
          replace: true,
          state: { message: res.message || 'Account registered successfully. Please sign in to enroll in 2FA.' }
        });
      } else {
        setError(res.error || 'Registration failed. Please check your details.');
      }
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || 'Registration failed.';
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
          <UserPlus size={24} />
        </div>
        <h2 style={{ fontSize: '22px', fontWeight: 800, color: '#0A2E65', marginBottom: '6px' }}>
          Customer Account Registration
        </h2>
        <p style={{ fontSize: '13px', color: '#64748B' }}>
          Register to apply for loans and access your NetBanking dashboard
        </p>
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

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
            Full Legal Name (as per PAN Card)
          </label>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
              <UserIcon size={18} />
            </div>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Ramesh Kumar Verma"
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
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
            Email Address
          </label>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
              <Mail size={18} />
            </div>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. ramesh.kumar@example.com"
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

        <div>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
            Choose Strong Password
          </label>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
              <Lock size={18} />
            </div>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Min 8 chars, 1 uppercase, 1 digit, 1 symbol"
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

        <div>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
            Confirm Password
          </label>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
              <Lock size={18} />
            </div>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Re-enter password"
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
          {loading ? 'Creating Account...' : 'Complete Registration'}
          {!loading && <ArrowRight size={18} />}
        </button>
      </form>

      <div style={{
        marginTop: '24px',
        paddingTop: '16px',
        borderTop: '1px solid #E2E8F0',
        textAlign: 'center',
        fontSize: '13px',
        color: '#64748B'
      }}>
        Already registered?{' '}
        <Link to="/login" style={{ color: '#0050A4', fontWeight: 700, textDecoration: 'none' }}>
          Sign In Here
        </Link>
      </div>
    </div>
  );
};

export default RegisterPage;
