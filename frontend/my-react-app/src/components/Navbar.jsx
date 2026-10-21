import React, { useState } from 'react';
import { performLogout } from '../services/authService';
import { getBackendUrl } from '../utils/apiConfig';

export default function Navbar({ authState }) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const { authenticated, user, csrfToken, csrfParameter } = authState;

  const handleLogout = (e) => {
    e.preventDefault();
    performLogout(csrfToken, csrfParameter);
  };

  const getRoleLabel = (role) => {
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
        return 'Admin';
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

  return (
    <header className="navbar-container">
      <div className="navbar-inner">
        {/* Brand Logo */}
        <div className="logo">
          <a href="/" className="logo-link">
            <img 
              src="/images/bank-logo.jpg" 
              alt="Bank of India Logo" 
              className="logo-img"
            />
            <span className="logo-text">Bank of <span className="brand-accent">India</span></span>
          </a>
        </div>

        {/* Mobile menu button */}
        <button 
          className="mobile-toggle"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label="Toggle navigation menu"
          aria-expanded={mobileMenuOpen}
        >
          <span className="hamburger-bar"></span>
          <span className="hamburger-bar"></span>
          <span className="hamburger-bar"></span>
        </button>

        {/* Navigation items */}
        <nav className={`nav-links ${mobileMenuOpen ? 'nav-mobile-open' : ''}`}>
          {authenticated && user ? (
            <>
              <div className="user-badge">
                <span className="user-icon" aria-hidden="true">👤</span>
                <span className="user-name">{user.name || user.email}</span>
                <span className="role-pill">{getRoleLabel(user.role)}</span>
              </div>

              <a href={getBackendUrl('/dashboard')} className="nav-item">Dashboard</a>

              {isStaffOrAdmin && (
                <a href={getBackendUrl('/loan-search')} className="nav-item">Loan Search</a>
              )}

              {isAdmin && (
                <>
                  <a href={getBackendUrl('/admin/staff')} className="nav-item">Staff Management</a>
                  <a href={getBackendUrl('/admin/audit-logs')} className="nav-item">Audit Trail</a>
                  <a href={getBackendUrl('/admin/reminders/demo')} className="nav-item nav-item-highlight">
                    🔔 DPD Reminders Demo
                  </a>
                </>
              )}

              {isCustomer && (
                <a href={getBackendUrl('/applyLoan')} className="nav-item nav-item-cta">Apply Loan</a>
              )}

              <button 
                type="button" 
                onClick={handleLogout}
                className="logout-btn"
                title="Log out of session"
              >
                Log Out
              </button>
            </>
          ) : (
            <>
              <a href="/" className="nav-item nav-item-active">Home</a>
              <a href="#calculator" className="nav-item" onClick={(e) => {
                const el = document.getElementById('calculator');
                if (el) {
                  e.preventDefault();
                  el.scrollIntoView({ behavior: 'smooth' });
                  setMobileMenuOpen(false);
                }
              }}>EMI Calculator</a>
              <a href="#features" className="nav-item" onClick={(e) => {
                const el = document.getElementById('features');
                if (el) {
                  e.preventDefault();
                  el.scrollIntoView({ behavior: 'smooth' });
                  setMobileMenuOpen(false);
                }
              }}>Loan Products</a>
              <a href={getBackendUrl('/login')} className="nav-item">Login</a>
              <a href={getBackendUrl('/register')} className="nav-btn-signup">Sign Up</a>
            </>
          )}
        </nav>
      </div>

      <style>{`
        .navbar-container {
          background: #FFFFFF;
          border-bottom: 1px solid var(--border-light);
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
          position: sticky;
          top: 0;
          z-index: 1000;
          width: 100%;
        }

        .navbar-inner {
          max-width: 1300px;
          margin: 0 auto;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 14px 28px;
        }

        .logo-link {
          font-size: 21px;
          font-weight: 800;
          color: var(--primary-navy);
          text-decoration: none;
          display: flex;
          align-items: center;
          gap: 12px;
          letter-spacing: -0.3px;
        }

        .logo-img {
          height: 38px;
          width: auto;
          object-fit: contain;
          border-radius: 4px;
        }

        .brand-accent {
          color: var(--accent-red);
        }

        .brand-orange {
          color: var(--accent-orange);
        }

        .nav-links {
          display: flex;
          align-items: center;
          gap: 18px;
        }

        .nav-item {
          text-decoration: none;
          color: var(--nav-link);
          font-weight: 600;
          font-size: 14px;
          padding: 8px 12px;
          border-radius: 6px;
          transition: all 0.15s ease-in-out;
        }

        .nav-item:hover {
          color: var(--accent-orange);
          background: #F8FAFC;
        }

        .nav-item-active {
          color: var(--accent-orange);
        }

        .nav-item-highlight {
          color: #d97706 !important;
          font-weight: 700;
        }

        .nav-item-cta {
          color: #FFFFFF !important;
          background: var(--accent-orange);
          padding: 8px 16px;
        }

        .nav-item-cta:hover {
          background: var(--accent-orange-hover) !important;
        }

        .nav-btn-signup {
          text-decoration: none;
          background: var(--accent-orange);
          color: #FFFFFF;
          font-weight: 700;
          font-size: 14px;
          padding: 8px 18px;
          border-radius: 6px;
          box-shadow: 0 2px 8px rgba(237, 125, 22, 0.3);
          transition: all 0.15s ease-in-out;
        }

        .nav-btn-signup:hover {
          background: var(--accent-orange-hover);
          transform: translateY(-1px);
        }

        .logout-btn {
          background: #FEE2E2;
          border: 1px solid #FECACA;
          color: #DC2626;
          font-weight: 600;
          cursor: pointer;
          font-size: 13px;
          padding: 6px 14px;
          border-radius: 6px;
          transition: all 0.15s ease-in-out;
          display: inline-flex;
          align-items: center;
        }

        .logout-btn:hover {
          background: #DC2626;
          color: #FFFFFF;
          border-color: #DC2626;
        }

        .mobile-toggle {
          display: none;
          flex-direction: column;
          gap: 5px;
          background: transparent;
          border: none;
          cursor: pointer;
          padding: 6px;
        }

        .hamburger-bar {
          width: 22px;
          height: 2.5px;
          background: var(--primary-navy);
          border-radius: 2px;
        }

        @media (max-width: 868px) {
          .mobile-toggle {
            display: flex;
          }

          .nav-links {
            display: none;
            position: absolute;
            top: 100%;
            left: 0;
            right: 0;
            background: #FFFFFF;
            flex-direction: column;
            align-items: flex-start;
            padding: 20px 24px;
            box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);
            border-bottom: 1px solid var(--border-light);
            gap: 12px;
          }

          .nav-links.nav-mobile-open {
            display: flex;
          }

          .nav-item, .nav-btn-signup, .logout-btn {
            width: 100%;
            text-align: left;
            padding: 10px 12px;
          }
        }
      `}</style>
    </header>
  );
}
