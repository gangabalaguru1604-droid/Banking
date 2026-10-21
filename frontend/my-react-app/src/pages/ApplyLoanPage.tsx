import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { loanApi } from '../api/loanApi';
import { useAuth } from '../context/AuthContext';
import {
  CreditCard,
  Calculator,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  TrendingUp,
  Info
} from 'lucide-react';

export const ApplyLoanPage: React.FC = () => {
  const { authState } = useAuth();
  const navigate = useNavigate();

  const [loadingTerms, setLoadingTerms] = useState(true);
  const [defaultRate, setDefaultRate] = useState<number>(8.5);
  const [minAmount, setMinAmount] = useState<number>(10000);
  const [maxAmount, setMaxAmount] = useState<number>(5000000);

  // Form State
  const [amount, setAmount] = useState<number>(100000);
  const [tenureMonths, setTenureMonths] = useState<number>(12);
  const [monthlyIncome, setMonthlyIncome] = useState<number>(50000);
  const [existingObligations, setExistingObligations] = useState<number>(0);
  const [agreeTerms, setAgreeTerms] = useState<boolean>(false);

  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function loadTerms() {
      try {
        const terms = await loanApi.getLoanTerms();
        if (isMounted && terms) {
          if (terms.defaultInterestRate) setDefaultRate(terms.defaultInterestRate);
          if (terms.minLoanAmount) setMinAmount(terms.minLoanAmount);
          if (terms.maxLoanAmount) setMaxAmount(terms.maxLoanAmount);
          if (terms.defaultTenureMonths) setTenureMonths(terms.defaultTenureMonths);
        }
      } catch (err) {
        console.warn('Could not load terms, using defaults:', err);
      } finally {
        if (isMounted) setLoadingTerms(false);
      }
    }
    loadTerms();
    return () => { isMounted = false; };
  }, []);

  // Live EMI calculation
  const calculateEmi = () => {
    if (amount <= 0 || tenureMonths <= 0 || defaultRate <= 0) return 0;
    const monthlyRate = defaultRate / 12 / 100;
    const emi = (amount * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)) /
      (Math.pow(1 + monthlyRate, tenureMonths) - 1);
    return Math.round(emi * 100) / 100;
  };

  const emi = calculateEmi();
  const totalPayment = emi * tenureMonths;
  const totalInterest = Math.max(0, totalPayment - amount);

  // DTI (Debt-to-Income) check
  const totalObligations = emi + (existingObligations || 0);
  const dtiRatio = monthlyIncome > 0 ? (totalObligations / monthlyIncome) * 100 : 0;

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!agreeTerms) {
      setError('You must agree to the Bank of India Fair Practices Code and credit inquiry authorization.');
      return;
    }

    if (amount < minAmount || amount > maxAmount) {
      setError(`Loan amount must be between ${formatCurrency(minAmount)} and ${formatCurrency(maxAmount)}.`);
      return;
    }

    setError(null);
    setSubmitting(true);

    try {
      const res = await loanApi.applyLoan({
        amount,
        tenureMonths,
        interest: defaultRate,
        monthlyIncome,
        existingObligations
      });

      if (res.success) {
        navigate('/dashboard', {
          replace: true,
          state: { message: `Loan application for ${formatCurrency(amount)} submitted successfully! Our credit committee is reviewing it.` }
        });
      } else {
        setError(res.error || 'Failed to submit loan application.');
      }
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || 'Loan application submission failed.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '1100px', margin: '32px auto', padding: '0 20px' }}>
      {/* Page Header */}
      <div style={{ marginBottom: '28px' }}>
        <Link to="/dashboard" style={{ fontSize: '13px', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
          ← Cancel & Back to Dashboard
        </Link>
        <h1 style={{ fontSize: '26px', fontWeight: 800, color: '#0A2E65', margin: '8px 0 4px' }}>
          Apply for a Smart Personal / Retail Loan
        </h1>
        <p style={{ fontSize: '14px', color: '#64748B', margin: 0 }}>
          Transparent institutional financing with real-time credit evaluation and instant EMI preview
        </p>
      </div>

      {/* Staff Policy Restriction */}
      {authState.user?.role && authState.user.role !== 'ROLE_CUSTOMER' && (
        <div style={{
          backgroundColor: '#FFFBEB',
          border: '1px solid #FEF3C7',
          color: '#B45309',
          padding: '16px 20px',
          borderRadius: '8px',
          marginBottom: '24px',
          fontSize: '14px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <AlertCircle size={22} color="#D97706" style={{ flexShrink: 0 }} />
          <div>
            <strong>Staff Policy Restriction:</strong> Internal institutional staff are prohibited from self-originating personal loans under Bank of India credit governance policy.
          </div>
        </div>
      )}

      {error && (
        <div style={{
          backgroundColor: '#FEF2F2',
          border: '1px solid #FEE2E2',
          color: '#991B1B',
          padding: '14px 16px',
          borderRadius: '8px',
          fontSize: '13px',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <AlertCircle size={20} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      <div style={{
        display: 'grid',
        gridTemplateColumns: '1.4fr 1fr',
        gap: '32px',
        alignItems: 'flex-start'
      }} className="apply-loan-grid">
        {/* Left Column: Form */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #E2E8F0',
          padding: '28px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#0A2E65', marginBottom: '20px' }}>
            1. Configure Loan Terms
          </h2>

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
            {/* Loan Amount Slider */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <label style={{ fontSize: '13px', fontWeight: 700, color: '#0A2E65' }}>
                  Required Loan Amount
                </label>
                <span style={{ fontSize: '18px', fontWeight: 800, color: '#0050A4' }}>
                  {formatCurrency(amount)}
                </span>
              </div>
              <input
                type="range"
                name="amount"
                id="amount"
                min={minAmount}
                max={Math.min(maxAmount, 1000000)}
                step={5000}
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                style={{ width: '100%', accentColor: '#0050A4', cursor: 'pointer' }}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#94A3B8', marginTop: '4px' }}>
                <span>Min: {formatCurrency(minAmount)}</span>
                <span>Max: {formatCurrency(maxAmount)}</span>
              </div>
            </div>

            {/* Tenure Options */}
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '8px' }}>
                Repayment Tenure (Months)
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '8px' }}>
                {[6, 12, 24, 36, 60].map((t) => (
                  <button
                    key={t}
                    type="button"
                    onClick={() => setTenureMonths(t)}
                    style={{
                      padding: '10px 4px',
                      borderRadius: '6px',
                      border: tenureMonths === t ? '2px solid #0050A4' : '1px solid #CBD5E1',
                      backgroundColor: tenureMonths === t ? '#EFF6FF' : '#FFFFFF',
                      color: tenureMonths === t ? '#0050A4' : '#475569',
                      fontWeight: tenureMonths === t ? 700 : 500,
                      fontSize: '13px',
                      cursor: 'pointer'
                    }}
                  >
                    {t} Mo
                  </button>
                ))}
              </div>
            </div>

            {/* Interest Rate Notice */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 14px',
              backgroundColor: '#F8FAFC',
              borderRadius: '6px',
              border: '1px solid #E2E8F0',
              fontSize: '13px'
            }}>
              <span style={{ color: '#475569' }}>Standard Applicable Interest Rate:</span>
              <strong style={{ color: '#16A34A', fontSize: '15px' }}>{defaultRate}% p.a. (Fixed)</strong>
            </div>

            {/* Income & Obligations */}
            <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#0A2E65', margin: '10px 0 0' }}>
              2. Income & Eligibility Details
            </h2>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                Monthly Net In-hand Income (₹)
              </label>
              <input
                type="number"
                min={10000}
                step={1000}
                value={monthlyIncome}
                onChange={(e) => setMonthlyIncome(Number(e.target.value))}
                required
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '14px',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                Existing Monthly Loan EMIs / Obligations (₹)
              </label>
              <input
                type="number"
                min={0}
                step={500}
                value={existingObligations}
                onChange={(e) => setExistingObligations(Number(e.target.value))}
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '14px',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            {/* Fair Practices Agreement */}
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', marginTop: '6px' }}>
              <input
                type="checkbox"
                id="agreeTerms"
                checked={agreeTerms}
                onChange={(e) => setAgreeTerms(e.target.checked)}
                style={{ marginTop: '3px', cursor: 'pointer', width: '16px', height: '16px', accentColor: '#0050A4' }}
              />
              <label htmlFor="agreeTerms" style={{ fontSize: '12px', color: '#475569', lineHeight: '1.5', cursor: 'pointer' }}>
                I confirm that all financial details provided are accurate. I authorize Bank of India to verify my credit history with CIBIL / Experian under the RBI Fair Practices Code.
              </label>
            </div>

            <button
              type="submit"
              disabled={submitting}
              style={{
                padding: '14px 20px',
                backgroundColor: '#ED7D16',
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
                boxShadow: '0 4px 12px rgba(237, 125, 22, 0.35)',
                marginTop: '10px'
              }}
            >
              {submitting ? 'Submitting Application...' : 'Submit Loan Application'}
              {!submitting && <ArrowRight size={18} />}
            </button>
          </form>
        </div>

        {/* Right Column: Live Repayment Summary & DTI Card */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div style={{
            backgroundColor: '#0A2E65',
            color: '#FFFFFF',
            borderRadius: '12px',
            padding: '24px',
            boxShadow: '0 8px 20px rgba(10, 46, 101, 0.2)'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <Calculator size={20} color="#ED7D16" />
              <h3 style={{ fontSize: '16px', fontWeight: 800, margin: 0 }}>
                EMI Summary Breakdown
              </h3>
            </div>

            <div style={{ textAlign: 'center', padding: '16px 0', borderBottom: '1px solid rgba(255, 255, 255, 0.1)' }}>
              <span style={{ fontSize: '12px', color: '#94A3B8', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Estimated Monthly EMI
              </span>
              <div style={{ fontSize: '32px', fontWeight: 800, color: '#38BDF8', marginTop: '4px' }}>
                {formatCurrency(emi)}
              </div>
              <span style={{ fontSize: '11px', color: '#CBD5E1' }}>
                for {tenureMonths} monthly installments
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '16px', fontSize: '13px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#94A3B8' }}>Principal Sanction:</span>
                <span style={{ fontWeight: 700 }}>{formatCurrency(amount)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#94A3B8' }}>Total Interest Payable:</span>
                <span style={{ fontWeight: 700, color: '#ED7D16' }}>{formatCurrency(totalInterest)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid rgba(255, 255, 255, 0.1)', paddingTop: '10px' }}>
                <span style={{ color: '#F8FAFC', fontWeight: 700 }}>Total Repayment:</span>
                <span style={{ fontWeight: 800, color: '#38BDF8', fontSize: '15px' }}>{formatCurrency(totalPayment)}</span>
              </div>
            </div>
          </div>

          {/* DTI Eligibility Card */}
          <div style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '12px',
            border: '1px solid #E2E8F0',
            padding: '20px',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
          }}>
            <h4 style={{ fontSize: '14px', fontWeight: 800, color: '#0A2E65', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <TrendingUp size={16} color="#0050A4" />
              Pre-Approval Eligibility Check
            </h4>
            <div style={{ fontSize: '13px', color: '#64748B', marginBottom: '12px' }}>
              Estimated Debt-to-Income (DTI): <strong>{Math.round(dtiRatio)}%</strong>
            </div>

            <div style={{
              height: '8px',
              backgroundColor: '#E2E8F0',
              borderRadius: '4px',
              overflow: 'hidden',
              marginBottom: '10px'
            }}>
              <div style={{
                height: '100%',
                width: `${Math.min(100, dtiRatio)}%`,
                backgroundColor: dtiRatio <= 40 ? '#16A34A' : (dtiRatio <= 60 ? '#ED7D16' : '#DC2626')
              }} />
            </div>

            <p style={{ fontSize: '12px', color: dtiRatio <= 50 ? '#166534' : '#991B1B', margin: 0 }}>
              {dtiRatio <= 50
                ? '✅ Excellent profile: DTI is within RBI recommended 50% limit.'
                : '⚠️ Notice: High obligation ratio. Consider increasing tenure to lower EMI.'}
            </p>
          </div>
        </div>
      </div>

      <style>{`
        @media (max-width: 900px) {
          .apply-loan-grid {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  );
};

export default ApplyLoanPage;
