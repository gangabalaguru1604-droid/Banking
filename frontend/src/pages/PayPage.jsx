import React, { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { initialLoans } from '../services/mockData';

export default function PayPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const loanId = Number(searchParams.get('loanId') || 101);

  const loan = initialLoans.find(l => l.id === loanId) || initialLoans[0];

  const [paymentType, setPaymentType] = useState('emi'); // 'emi', 'full', 'custom'
  const [method, setMethod] = useState('upi');
  const [loading, setLoading] = useState(false);
  const [customAmount, setCustomAmount] = useState(loan.monthlyEmi.toString());

  const totalPayable = paymentType === 'emi' 
    ? (loan.monthlyEmi + loan.penalty) 
    : paymentType === 'full' 
    ? (loan.remainingBalance + loan.penalty) 
    : Number(customAmount);

  const formatINR = (val) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);

  const handlePay = (e) => {
    e.preventDefault();
    setLoading(true);

    setTimeout(() => {
      setLoading(false);
      alert(`Payment of ${formatINR(totalPayable)} processed successfully via ${method.toUpperCase()}! Transaction ID: TXN-${Date.now().toString().slice(-6)}.`);
      navigate('/dashboard');
    }, 700);
  };

  return (
    <div className="container" style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '60px 20px' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '520px', padding: '40px 36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <span style={{ fontSize: '38px', display: 'block', marginBottom: '8px' }}>💳</span>
          <h2 style={{ fontFamily: 'var(--font-heading)', fontSize: '24px', fontWeight: 800, color: '#fff' }}>
            Loan EMI Repayment
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '13px', marginTop: '6px' }}>
            Account: <strong>{loan.loanNumber}</strong> ({loan.purpose})
          </p>
        </div>

        {loan.dpdDays > 0 && (
          <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.4)', color: '#fca5a5', padding: '14px', borderRadius: '12px', fontSize: '13px', marginBottom: '20px' }}>
            ⚠️ <strong>Overdue Account ({loan.dpdDays} Days Past Due):</strong> Late payment penalty of <strong>{formatINR(loan.penalty)}</strong> has been applied to this cycle.
          </div>
        )}

        <form onSubmit={handlePay}>
          {/* Payment Type Selection */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
              Repayment Option
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <button 
                type="button" 
                onClick={() => setPaymentType('emi')}
                style={{
                  background: paymentType === 'emi' ? 'rgba(37,99,235,0.2)' : 'rgba(255,255,255,0.04)',
                  border: '1px solid ' + (paymentType === 'emi' ? '#3b82f6' : 'rgba(255,255,255,0.1)'),
                  color: paymentType === 'emi' ? '#60a5fa' : '#94a3b8',
                  padding: '12px',
                  borderRadius: '10px',
                  cursor: 'pointer',
                  textAlign: 'left'
                }}
              >
                <div style={{ fontSize: '11px', textTransform: 'uppercase', fontWeight: 700 }}>Regular EMI</div>
                <div style={{ fontSize: '16px', fontWeight: 800, color: '#fff', marginTop: '2px' }}>{formatINR(loan.monthlyEmi)}</div>
              </button>

              <button 
                type="button" 
                onClick={() => setPaymentType('full')}
                style={{
                  background: paymentType === 'full' ? 'rgba(37,99,235,0.2)' : 'rgba(255,255,255,0.04)',
                  border: '1px solid ' + (paymentType === 'full' ? '#3b82f6' : 'rgba(255,255,255,0.1)'),
                  color: paymentType === 'full' ? '#60a5fa' : '#94a3b8',
                  padding: '12px',
                  borderRadius: '10px',
                  cursor: 'pointer',
                  textAlign: 'left'
                }}
              >
                <div style={{ fontSize: '11px', textTransform: 'uppercase', fontWeight: 700 }}>Full Foreclosure</div>
                <div style={{ fontSize: '16px', fontWeight: 800, color: '#fff', marginTop: '2px' }}>{formatINR(loan.remainingBalance)}</div>
              </button>
            </div>
          </div>

          {/* Payment Method */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
              Select Payment Gateway
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px' }}>
              {[
                { id: 'upi', label: '⚡ UPI / QR' },
                { id: 'netbanking', label: '🏦 NetBanking' },
                { id: 'card', label: '💳 Debit Card' }
              ].map(m => (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => setMethod(m.id)}
                  style={{
                    background: method === m.id ? 'rgba(59,130,246,0.2)' : 'rgba(255,255,255,0.04)',
                    border: '1px solid ' + (method === m.id ? '#60a5fa' : 'rgba(255,255,255,0.1)'),
                    color: method === m.id ? '#fff' : '#94a3b8',
                    padding: '10px 8px',
                    borderRadius: '8px',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          {/* Amount Due Box */}
          <div style={{ background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '14px', padding: '16px', marginBottom: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#94a3b8', marginBottom: '6px' }}>
              <span>Base Installment:</span>
              <span style={{ color: '#fff', fontWeight: 600 }}>{formatINR(paymentType === 'full' ? loan.remainingBalance : loan.monthlyEmi)}</span>
            </div>
            {loan.penalty > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#f59e0b', marginBottom: '6px' }}>
                <span>Late Fee Penalty:</span>
                <span style={{ fontWeight: 600 }}>+ {formatINR(loan.penalty)}</span>
              </div>
            )}
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '16px', fontWeight: 800, color: '#fff', borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '10px', marginTop: '6px' }}>
              <span>Total Payable Now:</span>
              <span style={{ color: '#34d399' }}>{formatINR(totalPayable)}</span>
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="btn-primary" 
            style={{ width: '100%', padding: '14px' }}
          >
            {loading ? 'Processing Payment...' : `Pay ${formatINR(totalPayable)} Securely ➔`}
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
