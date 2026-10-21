import React from 'react';
import { Outlet, Link } from 'react-router-dom';
import { ShieldCheck, Lock } from 'lucide-react';

export const AuthLayout: React.FC = () => {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      backgroundColor: '#F0F4F8',
      backgroundImage: 'radial-gradient(#CBD5E1 1px, transparent 1px)',
      backgroundSize: '24px 24px'
    }}>
      {/* Header Bar */}
      <header style={{
        padding: '20px 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid #E2E8F0',
        backgroundColor: '#FFFFFF'
      }}>
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none' }}>
          <img
            src="/images/bank-logo.jpg"
            alt="Bank of India"
            style={{ height: '36px', width: 'auto' }}
            onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }}
          />
          <span style={{ fontSize: '18px', fontWeight: 800, color: '#0050A4' }}>
            Bank of <span style={{ color: '#E31B23' }}>India</span>
          </span>
        </Link>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#16A34A', fontSize: '13px', fontWeight: 600 }}>
          <ShieldCheck size={18} />
          <span>Secure Net Banking</span>
        </div>
      </header>

      {/* Main Form Center */}
      <div style={{
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '32px 16px'
      }}>
        <div style={{
          maxWidth: '480px',
          width: '100%',
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 10px 25px rgba(0, 80, 164, 0.08)',
          overflow: 'hidden'
        }}>
          <div style={{
            height: '4px',
            backgroundColor: '#0050A4',
            backgroundImage: 'linear-gradient(90deg, #0050A4 0%, #ED7D16 100%)'
          }} />
          <div style={{ padding: '32px' }}>
            <Outlet />
          </div>
        </div>
      </div>

      {/* Footer */}
      <footer style={{
        padding: '16px',
        textAlign: 'center',
        fontSize: '12px',
        color: '#64748B'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', marginBottom: '6px' }}>
          <Lock size={12} />
          <span>Protected by 256-Bit TLS & RFC 6238 TOTP Multi-Factor Authentication</span>
        </div>
        <div>
          © {new Date().getFullYear()} Bank of India. All Rights Reserved.
        </div>
      </footer>
    </div>
  );
};
