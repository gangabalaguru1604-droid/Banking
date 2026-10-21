import React from 'react';
import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="footer-wrapper">
      <div className="container">
        <div className="footer-grid">
          {/* Brand Info */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
              <span style={{ fontSize: '26px' }}>🏦</span>
              <span style={{ fontFamily: 'var(--font-heading)', fontSize: '22px', fontWeight: 800, color: '#fff' }}>
                Indian Bank
              </span>
            </div>
            <p style={{ fontSize: '13px', color: '#94a3b8', lineHeight: 1.6, marginBottom: '18px', maxWidth: '320px' }}>
              A premier public sector banking institution with 100+ years of trust. Pioneering paperless, automated digital loan origination and repayments.
            </p>
            <div style={{ fontSize: '12px', color: '#64748b' }}>
              RBI Regulated • ISO 27001 Certified • 256-bit SSL
            </div>
          </div>

          {/* Quick Links */}
          <div className="footer-col">
            <h4>Digital Lending</h4>
            <ul className="footer-links">
              <li><Link to="/apply-loan">Apply for Loan</Link></li>
              <li><a href="#calculator">EMI Calculator</a></li>
              <li><a href="#loans">Personal Credit</a></li>
              <li><a href="#loans">MSME Term Loans</a></li>
              <li><a href="#loans">Home Loans</a></li>
            </ul>
          </div>

          {/* Operational Portals */}
          <div className="footer-col">
            <h4>Portals & Staff</h4>
            <ul className="footer-links">
              <li><Link to="/login">Staff & Customer Login</Link></li>
              <li><Link to="/register">Open Borrower Account</Link></li>
              <li><Link to="/dashboard">Operational Dashboard</Link></li>
              <li><Link to="/loan-search">Loan Search & Audit</Link></li>
              <li><Link to="/admin/staff">Staff Roles</Link></li>
            </ul>
          </div>

          {/* Customer Helpline */}
          <div className="footer-col">
            <h4>24x7 Support</h4>
            <ul className="footer-links">
              <li style={{ color: '#fff', fontWeight: 700 }}>📞 1800-200-8899</li>
              <li style={{ color: '#94a3b8', fontSize: '13px' }}>Toll-Free National Helpline</li>
              <li style={{ marginTop: '10px' }}>✉️ support@indianbank.co.in</li>
              <li>📍 Corporate Office: Chennai, India</li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <div>
            © 2026 Indian Bank. All Rights Reserved. Smart Digital Lending Core.
          </div>
          <div style={{ display: 'flex', gap: '20px' }}>
            <span>Privacy Policy</span>
            <span>Terms of Service</span>
            <span>Security Disclosures</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
