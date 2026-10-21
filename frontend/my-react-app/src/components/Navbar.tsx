import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Shield,
  LayoutDashboard,
  Search,
  Users,
  FileText,
  Bell,
  CreditCard,
  LogOut,
  Menu,
  X,
  UserCheck
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { authState, logout } = useAuth();
  const location = useLocation();

  const { authenticated, user } = authState;

  const getRoleLabel = (role?: string) => {
    switch (role) {
      case 'ROLE_CUSTOMER':
        return 'Customer';
      case 'ROLE_LOAN_OFFICER':
        return 'Loan Officer';
      case 'ROLE_COLLECTIONS_AGENT':
        return 'Collections Agent';
      case 'ROLE_COLLECTIONS_MANAGER':
        return 'Collections Manager';
      case 'ROLE_ADMIN':
        return 'Administrator';
      default:
        return role ? role.replace('ROLE_', '') : 'User';
    }
  };

  const isStaffOrAdmin = user && (
    user.role === 'ROLE_LOAN_OFFICER' ||
    user.role === 'ROLE_COLLECTIONS_AGENT' ||
    user.role === 'ROLE_COLLECTIONS_MANAGER' ||
    user.role === 'ROLE_ADMIN'
  );

  const isAdmin = user && user.role === 'ROLE_ADMIN';
  const isCustomer = user && user.role === 'ROLE_CUSTOMER';

  const isActive = (path: string) => location.pathname === path;

  return (
    <header style={{
      backgroundColor: '#FFFFFF',
      borderBottom: '1px solid #E2E8F0',
      position: 'sticky',
      top: 0,
      zIndex: 1000,
      boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
    }}>
      <div style={{
        maxWidth: '1280px',
        margin: '0 auto',
        padding: '0 20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: '72px'
      }}>
        {/* Brand Logo */}
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '12px', textDecoration: 'none' }}>
          <img
            src="/images/bank-logo.jpg"
            alt="Bank of India"
            style={{ height: '42px', width: 'auto', borderRadius: '4px' }}
            onError={(e) => {
              // Fallback if image not loaded
              (e.target as HTMLElement).style.display = 'none';
            }}
          />
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: '20px', fontWeight: 800, color: '#0050A4', letterSpacing: '-0.5px' }}>
              Bank of <span style={{ color: '#E31B23' }}>India</span>
            </span>
            <span style={{ fontSize: '11px', color: '#64748B', fontWeight: 600, letterSpacing: '0.5px' }}>
              SMART LOAN PLATFORM
            </span>
          </div>
        </Link>

        {/* Mobile menu button */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          style={{
            display: 'none',
            background: 'none',
            border: 'none',
            padding: '8px',
            cursor: 'pointer',
            color: '#0050A4'
          }}
          className="mobile-toggle-btn"
          aria-label="Toggle menu"
        >
          {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>

        {/* Desktop Nav Items */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '8px' }} className="nav-desktop">
          {authenticated && user ? (
            <>
              {/* User badge */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '6px 12px',
                backgroundColor: '#F1F5F9',
                borderRadius: '20px',
                marginRight: '8px'
              }}>
                <div style={{
                  width: '28px',
                  height: '28px',
                  borderRadius: '50%',
                  backgroundColor: '#0050A4',
                  color: '#FFFFFF',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '13px',
                  fontWeight: 700
                }}>
                  {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '13px', fontWeight: 600, color: '#0A2E65' }}>
                    {user.name || user.email}
                  </span>
                  <span style={{ fontSize: '10px', color: '#64748B', fontWeight: 600 }}>
                    {getRoleLabel(user.role)} {user.branch ? `• ${user.branch}` : ''}
                  </span>
                </div>
              </div>

              <Link
                to="/dashboard"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '8px 14px',
                  fontSize: '14px',
                  fontWeight: 600,
                  color: isActive('/dashboard') ? '#0050A4' : '#475569',
                  backgroundColor: isActive('/dashboard') ? '#EFF6FF' : 'transparent',
                  borderRadius: '6px',
                  textDecoration: 'none'
                }}
              >
                <LayoutDashboard size={16} />
                Dashboard
              </Link>

              {isStaffOrAdmin && (
                <Link
                  to="/loan-search"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    padding: '8px 14px',
                    fontSize: '14px',
                    fontWeight: 600,
                    color: isActive('/loan-search') ? '#0050A4' : '#475569',
                    backgroundColor: isActive('/loan-search') ? '#EFF6FF' : 'transparent',
                    borderRadius: '6px',
                    textDecoration: 'none'
                  }}
                >
                  <Search size={16} />
                  Loan Search
                </Link>
              )}

              {isAdmin && (
                <>
                  <Link
                    to="/admin/staff"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '8px 14px',
                      fontSize: '14px',
                      fontWeight: 600,
                      color: isActive('/admin/staff') ? '#0050A4' : '#475569',
                      backgroundColor: isActive('/admin/staff') ? '#EFF6FF' : 'transparent',
                      borderRadius: '6px',
                      textDecoration: 'none'
                    }}
                  >
                    <Users size={16} />
                    Staff
                  </Link>

                  <Link
                    to="/admin/audit-logs"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '8px 14px',
                      fontSize: '14px',
                      fontWeight: 600,
                      color: isActive('/admin/audit-logs') ? '#0050A4' : '#475569',
                      backgroundColor: isActive('/admin/audit-logs') ? '#EFF6FF' : 'transparent',
                      borderRadius: '6px',
                      textDecoration: 'none'
                    }}
                  >
                    <FileText size={16} />
                    Audit Trail
                  </Link>

                  <Link
                    to="/admin/reminders"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '8px 14px',
                      fontSize: '14px',
                      fontWeight: 600,
                      color: '#ED7D16',
                      backgroundColor: '#FFF7ED',
                      borderRadius: '6px',
                      textDecoration: 'none'
                    }}
                  >
                    <Bell size={16} />
                    DPD Reminders
                  </Link>
                </>
              )}

              {isCustomer && (
                <Link
                  to="/applyLoan"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    padding: '8px 16px',
                    fontSize: '14px',
                    fontWeight: 700,
                    color: '#FFFFFF',
                    backgroundColor: '#ED7D16',
                    borderRadius: '6px',
                    textDecoration: 'none',
                    boxShadow: '0 2px 6px rgba(237, 125, 22, 0.3)'
                  }}
                >
                  <CreditCard size={16} />
                  Apply Loan
                </Link>
              )}

              <button
                type="button"
                onClick={() => logout()}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '8px 14px',
                  fontSize: '14px',
                  fontWeight: 600,
                  color: '#DC2626',
                  backgroundColor: '#FEF2F2',
                  border: '1px solid #FEE2E2',
                  borderRadius: '6px',
                  cursor: 'pointer'
                }}
              >
                <LogOut size={16} />
                Logout
              </button>
            </>
          ) : (
            <>
              <Link
                to="/"
                style={{
                  padding: '8px 14px',
                  fontSize: '14px',
                  fontWeight: 600,
                  color: isActive('/') ? '#0050A4' : '#475569',
                  textDecoration: 'none'
                }}
              >
                Home
              </Link>
              <Link
                to="/login"
                style={{
                  padding: '8px 18px',
                  fontSize: '14px',
                  fontWeight: 600,
                  color: '#0050A4',
                  backgroundColor: '#EFF6FF',
                  borderRadius: '6px',
                  textDecoration: 'none'
                }}
              >
                Net Banking Login
              </Link>
              <Link
                to="/register"
                style={{
                  padding: '8px 18px',
                  fontSize: '14px',
                  fontWeight: 700,
                  color: '#FFFFFF',
                  backgroundColor: '#0050A4',
                  borderRadius: '6px',
                  textDecoration: 'none',
                  boxShadow: '0 2px 6px rgba(0, 80, 164, 0.25)'
                }}
              >
                Register
              </Link>
            </>
          )}
        </nav>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderBottom: '1px solid #E2E8F0',
          padding: '16px 20px',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px'
        }}>
          {authenticated && user ? (
            <>
              <div style={{ padding: '8px 0', borderBottom: '1px solid #E2E8F0' }}>
                <div style={{ fontWeight: 700, color: '#0A2E65' }}>{user.name || user.email}</div>
                <div style={{ fontSize: '12px', color: '#64748B' }}>{getRoleLabel(user.role)}</div>
              </div>
              <Link to="/dashboard" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
                Dashboard
              </Link>
              {isStaffOrAdmin && (
                <Link to="/loan-search" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
                  Loan Search
                </Link>
              )}
              {isAdmin && (
                <>
                  <Link to="/admin/staff" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
                    Staff Management
                  </Link>
                  <Link to="/admin/audit-logs" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
                    Audit Trail
                  </Link>
                  <Link to="/admin/reminders" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#ED7D16', fontWeight: 600, textDecoration: 'none' }}>
                    DPD Reminders
                  </Link>
                </>
              )}
              {isCustomer && (
                <Link to="/applyLoan" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#ED7D16', fontWeight: 700, textDecoration: 'none' }}>
                  Apply for Loan
                </Link>
              )}
              <button
                type="button"
                onClick={() => { setMobileMenuOpen(false); logout(); }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '10px 0',
                  color: '#DC2626',
                  background: 'none',
                  border: 'none',
                  fontWeight: 600,
                  cursor: 'pointer'
                }}
              >
                <LogOut size={16} /> Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
                Home
              </Link>
              <Link to="/login" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
                Login
              </Link>
              <Link to="/register" onClick={() => setMobileMenuOpen(false)} style={{ padding: '8px 0', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
                Register
              </Link>
            </>
          )}
        </div>
      )}

      <style>{`
        @media (max-width: 900px) {
          .nav-desktop {
            display: none !important;
          }
          .mobile-toggle-btn {
            display: block !important;
          }
        }
      `}</style>
    </header>
  );
};
