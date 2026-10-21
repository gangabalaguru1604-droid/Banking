import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { loanApi } from '../api/loanApi';
import { useAuth } from '../context/AuthContext';
import {
  CreditCard,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  Wallet,
  Clock,
  Printer
} from 'lucide-react';
import { Loan } from '../types';

export const RepaymentPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const loanIdParam = searchParams.get('loanId');
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [loan, setLoan] = useState<Loan | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Form State
  const [amount, setAmount] = useState<number>(0);
  const [paymentMode, setPaymentMode] = useState<string>('UPI');
  const [referenceNumber, setReferenceNumber] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [receipt, setReceipt] = useState<any>(null);

  useEffect(() => {
    let isMounted = true;
    async function loadDetails() {
      if (!loanIdParam) {
        setError('No loan account selected for repayment. Please select an active loan from your dashboard.');
        setLoading(false);
        return;
      }

      try {
        const data = await loanApi.getPaymentDetails(Number(loanIdParam));
        if (isMounted) {
          const l: Loan = data.loan || data;
          setLoan(l);
          const defaultPay = l.overdueAmount && l.overdueAmount > 0 ? l.overdueAmount : (l.emi || l.remaining);
          setAmount(Math.min(defaultPay, l.remaining));
          // Generate a mock realistic reference number for bank reconciliation
          setReferenceNumber(`BOI-${Date.now().toString().slice(-8)}`);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.response?.data?.error || err.message || 'Failed to load loan payment details.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    loadDetails();
    return () => { isMounted = false; };
  }, [loanIdParam]);

  const formatCurrency = (val: number | undefined) => {
    if (val === undefined || val === null) return '₹0.00';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2
    }).format(val);
  };

  const handlePay = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loan) return;

    if (amount <= 0) {
      setError('Payment amount must be greater than zero.');
      return;
    }

    if (amount > loan.remaining) {
      setError(`Payment cannot exceed the remaining loan balance of ${formatCurrency(loan.remaining)}.`);
      return;
    }

    setError(null);
    setSubmitting(true);

    try {
      const res = await loanApi.submitPayment({
        loanId: loan.id,
        amount,
        paymentMode,
        referenceNumber
      });

      if (res.success) {
        setReceipt({
          transactionId: res.transactionId || referenceNumber,
          amountPaid: amount,
          paymentDate: new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }),
          paymentMode,
          referenceNumber,
          remainingBalance: res.newRemainingBalance !== undefined ? res.newRemainingBalance : (loan.remaining - amount),
          loanId: loan.id
        });
      } else {
        setError(res.error || 'Payment processing failed.');
      }
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || 'Payment processing failed.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '800px', margin: '40px auto', padding: '0 20px', textAlign: 'center' }}>
        <div style={{
          width: '36px',
          height: '36px',
          border: '3px solid #E2E8F0',
          borderTopColor: '#0050A4',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite',
          margin: '0 auto 12px'
        }} />
        <p style={{ color: '#64748B' }}>Fetching loan payment details...</p>
      </div>
    );
  }

  if (receipt) {
    return (
      <div style={{ maxWidth: '600px', margin: '40px auto', padding: '0 20px' }}>
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #BBF7D0',
          boxShadow: '0 10px 25px rgba(22, 163, 74, 0.1)',
          overflow: 'hidden'
        }}>
          <div style={{
            backgroundColor: '#16A34A',
            color: '#FFFFFF',
            padding: '24px',
            textAlign: 'center'
          }}>
            <CheckCircle2 size={48} style={{ margin: '0 auto 8px' }} />
            <h2 style={{ fontSize: '22px', fontWeight: 800, margin: '0 0 4px' }}>
              Payment Successful!
            </h2>
            <p style={{ fontSize: '13px', margin: 0, opacity: 0.9 }}>
              Transaction successfully settled into Bank of India CBS
            </p>
          </div>

          <div style={{ padding: '28px' }}>
            <div style={{
              backgroundColor: '#F8FAFC',
              borderRadius: '8px',
              border: '1px solid #E2E8F0',
              padding: '16px',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
              fontSize: '13px',
              marginBottom: '24px'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#64748B' }}>Payment Receipt:</span>
                <strong style={{ color: '#0A2E65' }}>{receipt.transactionId}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#64748B' }}>Loan Account ID:</span>
                <strong style={{ color: '#0050A4' }}>#{receipt.loanId}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#64748B' }}>Amount Paid:</span>
                <strong style={{ color: '#16A34A', fontSize: '16px' }}>{formatCurrency(receipt.amountPaid)}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#64748B' }}>Payment Mode:</span>
                <strong>{receipt.paymentMode}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#64748B' }}>Bank Reference Ref:</span>
                <code>{receipt.referenceNumber}</code>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #E2E8F0', paddingTop: '10px' }}>
                <span style={{ color: '#0A2E65', fontWeight: 700 }}>New Remaining Balance:</span>
                <strong style={{ color: '#0050A4', fontSize: '15px' }}>{formatCurrency(receipt.remainingBalance)}</strong>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                type="button"
                onClick={() => window.print()}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '10px 18px',
                  backgroundColor: '#F1F5F9',
                  border: '1px solid #CBD5E1',
                  borderRadius: '6px',
                  color: '#475569',
                  fontWeight: 600,
                  fontSize: '13px',
                  cursor: 'pointer'
                }}
              >
                <Printer size={16} /> Print Receipt
              </button>

              <Link
                to="/dashboard"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '10px 20px',
                  backgroundColor: '#0050A4',
                  color: '#FFFFFF',
                  borderRadius: '6px',
                  textDecoration: 'none',
                  fontWeight: 700,
                  fontSize: '13px'
                }}
              >
                Go to Dashboard
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '800px', margin: '32px auto', padding: '0 20px' }}>
      <div style={{ marginBottom: '24px' }}>
        <Link to="/dashboard" style={{ fontSize: '13px', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
          ← Back to Dashboard
        </Link>
        <h1 style={{ fontSize: '24px', fontWeight: 800, color: '#0A2E65', margin: '8px 0 4px' }}>
          Loan Repayment Gateway
        </h1>
        <p style={{ fontSize: '14px', color: '#64748B', margin: 0 }}>
          Make instant digital loan payments via UPI, NEFT, IMPS or NetBanking
        </p>
      </div>

      {error && (
        <div style={{
          backgroundColor: '#FEF2F2',
          border: '1px solid #FEE2E2',
          color: '#991B1B',
          padding: '14px 16px',
          borderRadius: '8px',
          fontSize: '13px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <AlertCircle size={20} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {loan ? (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #E2E8F0',
          padding: '28px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          {/* Loan Overview Banner */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: '16px',
            padding: '16px',
            backgroundColor: '#F8FAFC',
            borderRadius: '8px',
            border: '1px solid #E2E8F0',
            marginBottom: '24px',
            fontSize: '13px'
          }}>
            <div>
              <span style={{ color: '#64748B', display: 'block' }}>Loan Account</span>
              <strong style={{ color: '#0050A4', fontSize: '16px' }}>#{loan.id}</strong>
            </div>
            <div>
              <span style={{ color: '#64748B', display: 'block' }}>Sanction Amount</span>
              <strong style={{ color: '#0A2E65' }}>{formatCurrency(loan.amount)}</strong>
            </div>
            <div>
              <span style={{ color: '#64748B', display: 'block' }}>Remaining Principal</span>
              <strong style={{ color: '#C2410C', fontSize: '16px' }}>{formatCurrency(loan.remaining)}</strong>
            </div>
            <div>
              <span style={{ color: '#64748B', display: 'block' }}>Monthly EMI</span>
              <strong style={{ color: '#0A2E65' }}>{formatCurrency(loan.emi)}</strong>
            </div>
          </div>

          <form onSubmit={handlePay} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                Payment Amount (₹)
              </label>
              <input
                type="number"
                min={1}
                max={loan.remaining}
                step={0.01}
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                required
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '18px',
                  fontWeight: 700,
                  color: '#0A2E65',
                  boxSizing: 'border-box'
                }}
              />
              <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                <button
                  type="button"
                  onClick={() => setAmount(loan.emi)}
                  style={{
                    padding: '4px 10px',
                    borderRadius: '4px',
                    border: '1px solid #CBD5E1',
                    backgroundColor: '#FFFFFF',
                    fontSize: '12px',
                    cursor: 'pointer'
                  }}
                >
                  Pay 1 Month EMI ({formatCurrency(loan.emi)})
                </button>
                <button
                  type="button"
                  onClick={() => setAmount(loan.remaining)}
                  style={{
                    padding: '4px 10px',
                    borderRadius: '4px',
                    border: '1px solid #CBD5E1',
                    backgroundColor: '#FFFFFF',
                    fontSize: '12px',
                    cursor: 'pointer'
                  }}
                >
                  Pay Full Balance ({formatCurrency(loan.remaining)})
                </button>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '8px' }}>
                Select Payment Mode
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '10px' }}>
                {['UPI', 'NEFT', 'IMPS', 'NetBanking', 'Debit Card'].map((mode) => (
                  <button
                    key={mode}
                    type="button"
                    onClick={() => setPaymentMode(mode)}
                    style={{
                      padding: '12px',
                      borderRadius: '6px',
                      border: paymentMode === mode ? '2px solid #0050A4' : '1px solid #CBD5E1',
                      backgroundColor: paymentMode === mode ? '#EFF6FF' : '#FFFFFF',
                      color: paymentMode === mode ? '#0050A4' : '#475569',
                      fontWeight: paymentMode === mode ? 700 : 500,
                      fontSize: '13px',
                      cursor: 'pointer'
                    }}
                  >
                    {mode}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                Bank Reference / UTR Number
              </label>
              <input
                type="text"
                value={referenceNumber}
                onChange={(e) => setReferenceNumber(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '14px',
                  color: '#0A2E65',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              style={{
                padding: '14px 20px',
                backgroundColor: '#16A34A',
                color: '#FFFFFF',
                border: 'none',
                borderRadius: '6px',
                fontSize: '15px',
                fontWeight: 700,
                cursor: submitting ? 'not-allowed' : 'pointer',
                opacity: submitting ? 0.8 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 4px 12px rgba(22, 163, 74, 0.3)',
                marginTop: '10px'
              }}
            >
              {submitting ? 'Processing Payment...' : `Authorize & Pay ${formatCurrency(amount)}`}
              {!submitting && <ArrowRight size={18} />}
            </button>
          </form>
        </div>
      ) : (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <p>Please select a loan from your dashboard.</p>
          <Link to="/dashboard" className="btn">Return to Dashboard</Link>
        </div>
      )}
    </div>
  );
};

export default RepaymentPage;
