import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ApplyLoanPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [amount, setAmount] = useState('50000');
  const [purpose, setPurpose] = useState('Personal Lifestyle & Renovation');
  const [guarantorName, setGuarantorName] = useState('');
  const [guarantorPhone, setGuarantorPhone] = useState('');
  const [guarantorRelation, setGuarantorRelation] = useState('Parent');
  const [loading, setLoading] = useState(false);

  const isStaff = user?.role && user.role !== 'ROLE_CUSTOMER';

  // Standard Indian Bank loan parameters
  const interestRate = 10.5;
  const tenureMonths = 24;

  const calculateEmi = (p) => {
    const principal = Number(p);
    if (!principal || principal <= 0) return 0;
    const r = (interestRate / 100) / 12;
    const n = tenureMonths;
    const emi = (principal * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
    return Math.round(emi);
  };

  const formatINR = (val) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);

  const handleSubmit = (e) => {
    e.preventDefault();
    setLoading(true);

    setTimeout(() => {
      setLoading(false);
      alert('Loan application of ' + formatINR(amount) + ' submitted successfully! A Loan Officer will review your request.');
      navigate('/dashboard');
    }, 600);
  };

  if (isStaff) {
    return (
      <div className="container" style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '60px 20px' }}>
        <div className="glass-panel" style={{ width: '100%', maxWidth: '520px', padding: '40px', textAlign: 'center' }}>
          <div style={{ fontSize: '48px', marginBottom: '14px' }}>🛡️</div>
          <h2 style={{ fontFamily: 'var(--font-heading)', fontSize: '24px', fontWeight: 800, color: '#fff', marginBottom: '12px' }}>
            Staff Operational Portal
          </h2>
          <div style={{ background: 'rgba(59, 130, 246, 0.15)', border: '1px solid rgba(59, 130, 246, 0.3)', color: '#93c5fd', padding: '16px', borderRadius: '12px', fontSize: '13px', lineHeight: 1.6, textAlign: 'left', marginBottom: '24px' }}>
            ℹ️ <strong>Staff Policy Restriction:</strong> Loan origination and customer loan applications cannot be submitted by staff accounts ({user?.role}). Loan applications must be initiated directly by registered customers through the borrower self-service portal.
          </div>
          <Link to="/dashboard" className="btn-primary" style={{ display: 'inline-block' }}>
            Return to Operational Dashboard
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '60px 20px' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '540px', padding: '40px 36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <span style={{ fontSize: '38px', display: 'block', marginBottom: '8px' }}>📝</span>
          <h2 style={{ fontFamily: 'var(--font-heading)', fontSize: '26px', fontWeight: 800, color: '#fff' }}>
            Apply for a Loan
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '13px', marginTop: '6px' }}>
            Submit your loan application under Indian Bank standard credit terms
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Loan Amount Input */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
              Loan Required Amount (₹) <span style={{ color: '#f87171' }}>*</span>
            </label>
            <input 
              type="number"
              min="100"
              max="10000000"
              step="1000"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="e.g. 50000"
              required
              style={{ width: '100%', padding: '12px 16px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '10px', color: '#fff', fontSize: '16px', fontWeight: 700, outline: 'none' }}
            />
            <div style={{ fontSize: '11px', color: '#64748b', marginTop: '6px' }}>
              Permitted range: ₹100 – ₹1,00,00,000 (Calculated Monthly EMI: <strong style={{ color: '#34d399' }}>{formatINR(calculateEmi(amount))}</strong>)
            </div>
          </div>

          {/* Standard Terms Card */}
          <div style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '14px', padding: '16px', marginBottom: '20px' }}>
            <div style={{ fontSize: '11px', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '10px' }}>
              🏛️ Predefined Terms (Indian Bank Standard Policy)
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div style={{ background: 'rgba(7,11,20,0.6)', padding: '10px 14px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.05)' }}>
                <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>Interest Rate</div>
                <div style={{ fontSize: '16px', fontWeight: 800, color: '#60a5fa', marginTop: '2px' }}>
                  {interestRate}% <span style={{ fontSize: '11px', color: '#34d399', fontWeight: 600 }}>(Fixed)</span>
                </div>
              </div>
              <div style={{ background: 'rgba(7,11,20,0.6)', padding: '10px 14px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.05)' }}>
                <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>Standard Tenure</div>
                <div style={{ fontSize: '16px', fontWeight: 800, color: '#60a5fa', marginTop: '2px' }}>
                  {tenureMonths} Months
                </div>
              </div>
            </div>
            <div style={{ fontSize: '11px', color: '#64748b', marginTop: '8px' }}>
              ℹ️ Rates and tenure are standardized by Indian Bank policy and calculated automatically.
            </div>
          </div>

          {/* Purpose */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
              Loan Purpose
            </label>
            <select
              value={purpose}
              onChange={(e) => setPurpose(e.target.value)}
              style={{ width: '100%', padding: '12px 16px', background: '#070b14', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '10px', color: '#fff', fontSize: '14px', outline: 'none' }}
            >
              <option value="Personal Lifestyle & Renovation">Personal Lifestyle & Renovation</option>
              <option value="Education & Academic Fees">Education & Academic Fees</option>
              <option value="Medical & Emergency Treatment">Medical & Emergency Treatment</option>
              <option value="Vehicle & Green EV Financing">Vehicle & Green EV Financing</option>
              <option value="MSME Working Capital">MSME Working Capital</option>
            </select>
          </div>

          {/* Guarantor Details */}
          <div style={{ borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '16px', marginBottom: '24px' }}>
            <div style={{ fontSize: '12px', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '12px' }}>
              Guarantor / Reference Information
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', color: '#cbd5e1', marginBottom: '6px' }}>Guarantor Name</label>
                <input 
                  type="text" 
                  value={guarantorName} 
                  onChange={(e) => setGuarantorName(e.target.value)} 
                  placeholder="e.g. Ramesh Patil" 
                  style={{ width: '100%', padding: '10px 12px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '12px', color: '#cbd5e1', marginBottom: '6px' }}>Contact Phone</label>
                <input 
                  type="text" 
                  value={guarantorPhone} 
                  onChange={(e) => setGuarantorPhone(e.target.value)} 
                  placeholder="+91 98765 00000" 
                  style={{ width: '100%', padding: '10px 12px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
                />
              </div>
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="btn-primary" 
            style={{ width: '100%', padding: '13px' }}
          >
            {loading ? 'Submitting Application...' : 'Submit Loan Application ➔'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '20px' }}>
          <Link to="/dashboard" style={{ fontSize: '13px', color: '#94a3b8', textDecoration: 'none' }}>
            Cancel & Return to Dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}
