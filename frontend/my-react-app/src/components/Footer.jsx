import React from 'react';
import { getBackendUrl } from '../utils/apiConfig';

export default function Footer() {
  return (
    <footer className="footer-container">
      <div className="footer-inner">
        {/* Brand & mission */}
        <div className="footer-brand-col">
          <div className="footer-logo">
            <img 
              src="/images/bank-logo.jpg" 
              alt="Bank of India" 
              className="footer-logo-img" 
            />
            <span className="logo-text">Bank of <span className="brand-accent">India</span></span>
          </div>
          <p className="footer-desc">
            Bank of India Smart Loan Management System delivering transparent, compliant, and streamlined lending services across India.
          </p>
          <div className="footer-helpdesk">
            <span className="helpdesk-icon" aria-hidden="true">📞</span>
            <div>
              <span className="helpdesk-label">Customer Helpdesk (Toll Free)</span>
              <strong className="helpdesk-number">1800-200-8899</strong>
            </div>
          </div>
        </div>

        {/* Quick Links */}
        <div className="footer-links-col">
          <h4 className="footer-heading">Services</h4>
          <ul className="footer-link-list">
            <li><a href={getBackendUrl('/applyLoan')}>Apply for a Loan</a></li>
            <li><a href="/#calculator">EMI Calculator</a></li>
            <li><a href={getBackendUrl('/login')}>Customer Portal</a></li>
            <li><a href={getBackendUrl('/dashboard')}>Repayment Schedule</a></li>
          </ul>
        </div>

        {/* Security & Access */}
        <div className="footer-links-col">
          <h4 className="footer-heading">Portals & Security</h4>
          <ul className="footer-link-list">
            <li><a href={getBackendUrl('/login')}>Staff Sign In</a></li>
            <li><a href={getBackendUrl('/register')}>Open New Account</a></li>
            <li><a href={getBackendUrl('/forgot-password')}>Reset Password</a></li>
            <li><a href={getBackendUrl('/mfa-verify')}>MFA Authentication</a></li>
          </ul>
        </div>

        {/* Security Disclaimer */}
        <div className="footer-security-col">
          <h4 className="footer-heading">Compliance & Security</h4>
          <div className="security-notice-box">
            <span className="lock-icon" aria-hidden="true">🔒</span>
            <p>
              All sessions are secured via HTTPS, AES-GCM data encryption, and Multi-Factor Authenticator (TOTP) verification.
            </p>
          </div>
          <p className="notice-sub">
            Inactivity timeout is automatically enforced after 10 minutes of non-usage.
          </p>
        </div>
      </div>

      <div className="footer-bottom">
        <div className="footer-bottom-inner">
          <p>© {new Date().getFullYear()} Bank of India. All rights reserved. Smart Loan Management System.</p>
          <div className="bottom-links">
            <a href={getBackendUrl('/jsp-home')}>Legacy JSP View</a>
            <span>•</span>
            <a href={getBackendUrl('/login')}>Security Center</a>
            <span>•</span>
            <a href={getBackendUrl('/health')}>System Status</a>
          </div>
        </div>
      </div>

      <style>{`
        .footer-container {
          background: #0A2E65;
          color: #E2E8F0;
          padding-top: 60px;
          border-top: 1px solid #1D4ED8;
        }

        .footer-inner {
          max-width: 1240px;
          margin: 0 auto;
          padding: 0 24px 48px;
          display: grid;
          grid-template-columns: 1.4fr 0.8fr 0.8fr 1.2fr;
          gap: 40px;
        }

        .footer-logo {
          font-size: 22px;
          font-weight: 800;
          color: #FFFFFF;
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 14px;
        }

        .footer-logo-img {
          height: 38px;
          width: auto;
          object-fit: contain;
          border-radius: 4px;
          background: #FFFFFF;
          padding: 2px 4px;
        }

        .brand-accent {
          color: var(--accent-red);
        }

        .brand-orange {
          color: var(--accent-orange);
        }

        .footer-desc {
          color: #94A3B8;
          font-size: 14px;
          line-height: 1.6;
          margin: 0 0 20px;
          max-width: 320px;
        }

        .footer-helpdesk {
          display: flex;
          align-items: center;
          gap: 12px;
          background: rgba(255, 255, 255, 0.05);
          border: 1px solid rgba(255, 255, 255, 0.1);
          padding: 10px 14px;
          border-radius: 8px;
          max-width: 280px;
        }

        .helpdesk-icon {
          font-size: 20px;
        }

        .helpdesk-label {
          display: block;
          font-size: 11px;
          color: #94A3B8;
          text-transform: uppercase;
          letter-spacing: 0.3px;
        }

        .helpdesk-number {
          display: block;
          color: #FFFFFF;
          font-size: 15px;
        }

        .footer-heading {
          font-size: 15px;
          font-weight: 700;
          color: #FFFFFF;
          margin: 0 0 18px;
          letter-spacing: 0.2px;
        }

        .footer-link-list {
          list-style: none;
          padding: 0;
          margin: 0;
        }

        .footer-link-list li {
          margin-bottom: 10px;
        }

        .footer-link-list a {
          color: #94A3B8;
          text-decoration: none;
          font-size: 14px;
          transition: color 0.15s ease;
        }

        .footer-link-list a:hover {
          color: var(--accent-orange);
        }

        .security-notice-box {
          display: flex;
          gap: 10px;
          background: rgba(237, 125, 22, 0.1);
          border: 1px solid rgba(237, 125, 22, 0.3);
          padding: 12px;
          border-radius: 8px;
          margin-bottom: 10px;
        }

        .security-notice-box p {
          margin: 0;
          font-size: 12px;
          color: #F8FAFC;
          line-height: 1.5;
        }

        .notice-sub {
          font-size: 11px;
          color: #94A3B8;
          margin: 0;
        }

        .footer-bottom {
          border-top: 1px solid rgba(255, 255, 255, 0.1);
          background: #0F172A;
          padding: 20px 24px;
        }

        .footer-bottom-inner {
          max-width: 1240px;
          margin: 0 auto;
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 12px;
          font-size: 13px;
          color: #94A3B8;
        }

        .footer-bottom-inner p {
          margin: 0;
        }

        .bottom-links {
          display: flex;
          gap: 12px;
          align-items: center;
        }

        .bottom-links a {
          color: #94A3B8;
          text-decoration: none;
          font-size: 12px;
        }

        .bottom-links a:hover {
          color: var(--accent-orange);
        }

        @media (max-width: 900px) {
          .footer-inner {
            grid-template-columns: 1fr 1fr;
            gap: 32px;
          }
        }

        @media (max-width: 560px) {
          .footer-inner {
            grid-template-columns: 1fr;
          }

          .footer-bottom-inner {
            flex-direction: column;
            text-align: center;
          }
        }
      `}</style>
    </footer>
  );
}
