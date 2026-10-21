import React from 'react';
import { ShieldCheck, Phone, Mail, MapPin, Lock } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer style={{
      backgroundColor: '#0A2E65',
      color: '#FFFFFF',
      padding: '48px 20px 24px',
      marginTop: 'auto'
    }}>
      <div style={{
        maxWidth: '1280px',
        margin: '0 auto',
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
        gap: '32px',
        marginBottom: '40px'
      }}>
        {/* Column 1: Brand & Security */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
            <span style={{ fontSize: '20px', fontWeight: 800, color: '#FFFFFF' }}>
              Bank of <span style={{ color: '#ED7D16' }}>India</span>
            </span>
          </div>
          <p style={{ fontSize: '13px', color: '#94A3B8', lineHeight: '1.6', marginBottom: '16px' }}>
            Smart Loan Management System powered by secure RFC 6238 TOTP two-factor authentication,
            RBI-compliant DPD tracking, and end-to-end encryption.
          </p>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#38BDF8', fontSize: '13px', fontWeight: 600 }}>
            <ShieldCheck size={18} />
            <span>256-Bit SSL Secured Banking</span>
          </div>
        </div>

        {/* Column 2: Quick Links */}
        <div>
          <h4 style={{ fontSize: '15px', fontWeight: 700, marginBottom: '16px', color: '#F8FAFC' }}>
            Loan Products
          </h4>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '13px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <li><a href="/#calculator" style={{ color: '#94A3B8', textDecoration: 'none' }}>Personal Loans @ 8.5% p.a.</a></li>
            <li><a href="/#calculator" style={{ color: '#94A3B8', textDecoration: 'none' }}>Home Loans @ 8.4% p.a.</a></li>
            <li><a href="/#calculator" style={{ color: '#94A3B8', textDecoration: 'none' }}>Vehicle Loans @ 8.7% p.a.</a></li>
            <li><a href="/#calculator" style={{ color: '#94A3B8', textDecoration: 'none' }}>SME & Business Credit Lines</a></li>
            <li><a href="/#calculator" style={{ color: '#94A3B8', textDecoration: 'none' }}>Live EMI Calculator</a></li>
          </ul>
        </div>

        {/* Column 3: Customer Security Tips */}
        <div>
          <h4 style={{ fontSize: '15px', fontWeight: 700, marginBottom: '16px', color: '#F8FAFC' }}>
            Security & Safe Banking
          </h4>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '13px', display: 'flex', flexDirection: 'column', gap: '10px', color: '#94A3B8' }}>
            <li style={{ display: 'flex', alignItems: 'flex-start', gap: '6px' }}>
              <Lock size={14} style={{ marginTop: '2px', flexShrink: 0, color: '#ED7D16' }} />
              <span>Never share OTP, passwords, or TOTP keys with anyone.</span>
            </li>
            <li style={{ display: 'flex', alignItems: 'flex-start', gap: '6px' }}>
              <Lock size={14} style={{ marginTop: '2px', flexShrink: 0, color: '#ED7D16' }} />
              <span>Bank never requests sensitive details via SMS or phone calls.</span>
            </li>
            <li style={{ display: 'flex', alignItems: 'flex-start', gap: '6px' }}>
              <Lock size={14} style={{ marginTop: '2px', flexShrink: 0, color: '#ED7D16' }} />
              <span>Session automatically locks after 10 minutes of inactivity.</span>
            </li>
          </ul>
        </div>

        {/* Column 4: 24x7 Help Desk */}
        <div>
          <h4 style={{ fontSize: '15px', fontWeight: 700, marginBottom: '16px', color: '#F8FAFC' }}>
            24x7 Customer Support
          </h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '13px', color: '#94A3B8' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Phone size={16} color="#ED7D16" />
              <span>Toll Free: 1800-220-229 / 1800-103-1906</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Mail size={16} color="#ED7D16" />
              <span>loansupport@bankofindia.co.in</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
              <MapPin size={16} color="#ED7D16" style={{ flexShrink: 0, marginTop: '2px' }} />
              <span>Head Office: Star House, C-5, G Block, Bandra Kurla Complex, Mumbai - 400051</span>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Bar */}
      <div style={{
        maxWidth: '1280px',
        margin: '0 auto',
        paddingTop: '20px',
        borderTop: '1px solid rgba(255, 255, 255, 0.1)',
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '12px',
        fontSize: '12px',
        color: '#64748B'
      }}>
        <div>
          © {new Date().getFullYear()} Bank of India. All Rights Reserved. Licensed by Reserve Bank of India (RBI).
        </div>
        <div style={{ display: 'flex', gap: '16px' }}>
          <span>Privacy Policy</span>
          <span>Terms & Conditions</span>
          <span>RBI Fair Practices Code</span>
          <span>Customer Grievance Redressal</span>
        </div>
      </div>
    </footer>
  );
};
