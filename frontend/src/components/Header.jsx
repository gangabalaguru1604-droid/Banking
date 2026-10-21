import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Header() {
  const { user, logout, refreshSession, sessionTimeRemaining, switchRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const formatSeconds = (sec) => {
    const mins = Math.floor(sec / 60);
    const s = sec % 60;
    return `${mins < 10 ? '0' : ''}${mins}:${s < 10 ? '0' : ''}${s}`;
  };

  const getRoleBadgeLabel = (role) => {
    switch (role) {
      case 'ROLE_CUSTOMER': return 'Customer';
      case 'ROLE_LOAN_OFFICER': return 'Loan Officer';
      case 'ROLE_COLLECTIONS_AGENT': return 'Collections Agent';
      case 'ROLE_COLLECTIONS_MANAGER': return 'Collections Manager';
      case 'ROLE_ADMIN': return 'Administrator';
      default: return role || 'Guest';
    }
  };

  return (
    <header style={{
      position: 'sticky',
      top: 0,
      zIndex: 100,
      background: 'rgba(7, 11, 20, 0.92)',
      backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)',
      borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
      padding: '14px 0'
    }}>
      <div className="container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '20px' }}>
        {/* Brand */}
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none', color: '#fff' }}>
          <span style={{ fontSize: '26px' }}>🏦</span>
          <span style={{ fontFamily: 'var(--font-heading)', fontSize: '22px', fontWeight: 800, letterSpacing: '-0.5px' }}>
            Indian Bank
          </span>
          <span style={{ background: 'rgba(37,99,235,0.2)', border: '1px solid rgba(59,130,246,0.35)', color: '#60a5fa', fontSize: '10px', fontWeight: 700, padding: '2px 8px', borderRadius: '6px', textTransform: 'uppercase' }}>
            React Core
          </span>
        </Link>

        {/* Dynamic Navigation Links based on user role */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          <Link to="/" style={{ color: location.pathname === '/' ? '#60a5fa' : '#94a3b8', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>Home</Link>

          {user ? (
            <>
              <Link to="/dashboard" style={{ color: location.pathname === '/dashboard' ? '#60a5fa' : '#94a3b8', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>Dashboard</Link>

              {user.role === 'ROLE_CUSTOMER' && (
                <Link to="/apply-loan" style={{ color: location.pathname === '/apply-loan' ? '#60a5fa' : '#94a3b8', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>Apply Loan</Link>
              )}

              {(user.role === 'ROLE_LOAN_OFFICER' || user.role === 'ROLE_COLLECTIONS_AGENT' || user.role === 'ROLE_COLLECTIONS_MANAGER' || user.role === 'ROLE_ADMIN') && (
                <Link to="/loan-search" style={{ color: location.pathname === '/loan-search' ? '#60a5fa' : '#94a3b8', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>Loan Search</Link>
              )}

              {user.role === 'ROLE_ADMIN' && (
                <>
                  <Link to="/admin/staff" style={{ color: location.pathname === '/admin/staff' ? '#60a5fa' : '#94a3b8', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>Staff</Link>
                  <Link to="/admin/audits" style={{ color: location.pathname === '/admin/audits' ? '#60a5fa' : '#94a3b8', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>Audit Trail</Link>
                  <Link to="/admin/reminders" style={{ color: '#f59e0b', textDecoration: 'none', fontSize: '14px', fontWeight: 700 }}>🔔 DPD Reminders</Link>
                </>
              )}
            </>
          ) : (
            <>
              <Link to="/login" style={{ color: location.pathname === '/login' ? '#60a5fa' : '#94a3b8', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>Staff & Customer Login</Link>
              <Link to="/register" style={{ color: location.pathname === '/register' ? '#60a5fa' : '#94a3b8', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>Sign Up</Link>
            </>
          )}
        </nav>

        {/* User Badge, Demo Role Switcher & Action */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          {/* Quick Demo Role Switcher for instant testing */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(255,255,255,0.05)', padding: '4px 10px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.1)' }}>
            <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>Demo Role:</span>
            <select
              value={user?.role || ''}
              onChange={(e) => switchRole(e.target.value)}
              style={{ background: 'transparent', color: '#93c5fd', border: 'none', fontSize: '12px', fontWeight: 700, cursor: 'pointer', outline: 'none' }}
              title="Switch demo persona instantly"
            >
              <option value="ROLE_CUSTOMER" style={{ background: '#0f172a', color: '#fff' }}>Customer (Rajesh)</option>
              <option value="ROLE_LOAN_OFFICER" style={{ background: '#0f172a', color: '#fff' }}>Loan Officer (Arun)</option>
              <option value="ROLE_COLLECTIONS_AGENT" style={{ background: '#0f172a', color: '#fff' }}>Collections Agent (Pooja)</option>
              <option value="ROLE_ADMIN" style={{ background: '#0f172a', color: '#fff' }}>Bank Admin (Sanjay)</option>
            </select>
          </div>

          {user ? (
            <>
              {/* Session timer */}
              <div
                onClick={refreshSession}
                title="Click to refresh 10-minute session"
                style={{ fontSize: '12px', color: sessionTimeRemaining < 120 ? '#ef4444' : '#94a3b8', background: 'rgba(255,255,255,0.04)', padding: '6px 10px', borderRadius: '8px', cursor: 'pointer', border: '1px solid rgba(255,255,255,0.08)' }}
              >
                ⏱️ {formatSeconds(sessionTimeRemaining)}
              </div>

              {/* User Pill */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'rgba(15,23,42,0.8)', border: '1px solid rgba(255,255,255,0.1)', padding: '6px 14px', borderRadius: '20px' }}>
                <span style={{ fontSize: '13px', fontWeight: 600, color: '#fff' }}>{user.name}</span>
                <span style={{ background: user.role === 'ROLE_ADMIN' ? 'rgba(239,68,68,0.2)' : user.role === 'ROLE_CUSTOMER' ? 'rgba(16,185,129,0.2)' : 'rgba(59,130,246,0.2)', color: user.role === 'ROLE_ADMIN' ? '#f87171' : user.role === 'ROLE_CUSTOMER' ? '#34d399' : '#60a5fa', fontSize: '11px', fontWeight: 700, padding: '2px 8px', borderRadius: '12px' }}>
                  {getRoleBadgeLabel(user.role)}
                </span>
              </div>

              {/* Logout button */}
              <button
                type="button"
                onClick={handleLogout}
                style={{ background: 'transparent', border: '1px solid rgba(239,68,68,0.3)', color: '#f87171', padding: '6px 12px', borderRadius: '8px', fontSize: '12px', fontWeight: 600, cursor: 'pointer' }}
              >
                Sign Out
              </button>
            </>
          ) : (
            <Link to="/login" className="btn-primary" style={{ padding: '8px 18px', fontSize: '13px' }}>
              Sign In
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
