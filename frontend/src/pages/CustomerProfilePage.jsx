import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function CustomerProfilePage() {
  const { user } = useAuth();

  return (
    <div className="container" style={{ padding: '40px 20px 80px', maxWidth: '800px' }}>
      <div style={{ marginBottom: '28px' }}>
        <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#fff' }}>
          Borrower Identity & KYC Profile
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '14px', marginTop: '4px' }}>
          Official Indian Bank Customer Master Record
        </p>
      </div>

      <div className="glass-panel" style={{ padding: '36px', marginBottom: '28px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px', paddingBottom: '24px', borderBottom: '1px solid rgba(255,255,255,0.08)', marginBottom: '24px' }}>
          <div style={{ width: '64px', height: '64px', borderRadius: '50%', background: 'linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '28px' }}>
            👤
          </div>
          <div>
            <h2 style={{ fontFamily: 'var(--font-heading)', fontSize: '22px', fontWeight: 700, color: '#fff' }}>
              {user?.name || 'Rajesh Patil'}
            </h2>
            <div style={{ fontSize: '13px', color: '#60a5fa', fontWeight: 600, marginTop: '2px' }}>
              {user?.email || 'rajesh.patil@bank.com'} • Customer ID: #CUST-9921
            </div>
            <div style={{ marginTop: '6px' }}>
              <span style={{ background: 'rgba(16,185,129,0.2)', color: '#34d399', fontSize: '11px', fontWeight: 800, padding: '3px 8px', borderRadius: '6px' }}>
                ✓ KYC FULLY VERIFIED (PAN & AADHAAR)
              </span>
            </div>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', fontSize: '14px' }}>
          <div>
            <span style={{ color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>Phone Number:</span>
            <div style={{ fontWeight: 600, color: '#fff', marginTop: '2px' }}>+91 98765 43210 (Verified)</div>
          </div>
          <div>
            <span style={{ color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>PAN Card:</span>
            <div style={{ fontWeight: 600, color: '#fff', marginTop: '2px' }}>ABCDE••••F</div>
          </div>
          <div>
            <span style={{ color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>Aadhaar Number:</span>
            <div style={{ fontWeight: 600, color: '#fff', marginTop: '2px' }}>•••• •••• 9812</div>
          </div>
          <div>
            <span style={{ color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>Communication Address:</span>
            <div style={{ fontWeight: 600, color: '#fff', marginTop: '2px' }}>402, Shivajinagar, Pune, Maharashtra 411005</div>
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
        <Link to="/dashboard" className="btn-secondary">Back to Dashboard</Link>
        <Link to="/apply-loan" className="btn-primary">Apply for New Loan</Link>
      </div>
    </div>
  );
}
