import React, { useState, useMemo } from 'react';
import { getBackendUrl } from '../utils/apiConfig';

// Reusable currency formatter instantiated once to avoid garbage collection churn during slider drag
const inrCurrencyFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0
});

export default function EmiCalculator({ authState }) {
  const { authenticated } = authState;

  // Defaults based on application.properties predefined loan product settings
  const [amount, setAmount] = useState(500000);
  const [interestRate, setInterestRate] = useState(10.5);
  const [tenureMonths, setTenureMonths] = useState(24);

  // EMI calculation logic
  const { monthlyEmi, totalInterest, totalPayment, interestRatio } = useMemo(() => {
    const p = parseFloat(amount) || 0;
    const r = (parseFloat(interestRate) || 0) / (12 * 100);
    const n = parseInt(tenureMonths, 10) || 1;

    if (p <= 0 || r <= 0 || n <= 0) {
      return { monthlyEmi: 0, totalInterest: 0, totalPayment: 0, interestRatio: 0 };
    }

    const emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
    const total = emi * n;
    const interest = total - p;
    const ratio = Math.min(100, Math.max(0, (interest / total) * 100));

    return {
      monthlyEmi: Math.round(emi),
      totalInterest: Math.round(interest),
      totalPayment: Math.round(total),
      interestRatio: Math.round(ratio)
    };
  }, [amount, interestRate, tenureMonths]);

  const formatCurrency = (val) => inrCurrencyFormatter.format(val);

  const applyUrl = getBackendUrl(authenticated ? '/applyLoan' : '/register');

  return (
    <section id="calculator" className="calculator-section">
      <div className="calculator-container">
        <div className="section-header">
          <span className="section-pill">Interactive Financial Tool</span>
          <h2 className="section-title">Loan EMI Calculator</h2>
          <p className="section-subtitle">
            Calculate your monthly repayment installments, total interest, and plan your borrowing with 100% transparency.
          </p>
        </div>

        <div className="calculator-card">
          {/* Inputs Column */}
          <div className="calc-inputs-col">
            {/* Loan Amount */}
            <div className="calc-control-group">
              <div className="control-header">
                <label htmlFor="amount-input">Loan Amount</label>
                <div className="display-box">
                  <span className="currency-symbol">₹</span>
                  <input
                    id="amount-input"
                    type="number"
                    min="10000"
                    max="10000000"
                    step="10000"
                    value={amount}
                    onChange={(e) => setAmount(Number(e.target.value))}
                    className="calc-num-input"
                  />
                </div>
              </div>
              <input
                type="range"
                min="10000"
                max="10000000"
                step="10000"
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className="calc-range-slider"
              />
              <div className="range-bounds">
                <span>₹10,000</span>
                <span>₹1,00,00,000</span>
              </div>
            </div>

            {/* Interest Rate */}
            <div className="calc-control-group">
              <div className="control-header">
                <label htmlFor="rate-input">Interest Rate (p.a.)</label>
                <div className="display-box">
                  <input
                    id="rate-input"
                    type="number"
                    min="5"
                    max="25"
                    step="0.1"
                    value={interestRate}
                    onChange={(e) => setInterestRate(Number(e.target.value))}
                    className="calc-num-input"
                  />
                  <span className="unit-symbol">%</span>
                </div>
              </div>
              <input
                type="range"
                min="5"
                max="25"
                step="0.1"
                value={interestRate}
                onChange={(e) => setInterestRate(Number(e.target.value))}
                className="calc-range-slider"
              />
              <div className="range-bounds">
                <span>5.0%</span>
                <span>Default: 10.5%</span>
                <span>25.0%</span>
              </div>
            </div>

            {/* Tenure */}
            <div className="calc-control-group">
              <div className="control-header">
                <label htmlFor="tenure-input">Loan Tenure</label>
                <div className="display-box">
                  <input
                    id="tenure-input"
                    type="number"
                    min="6"
                    max="120"
                    step="1"
                    value={tenureMonths}
                    onChange={(e) => setTenureMonths(Number(e.target.value))}
                    className="calc-num-input"
                  />
                  <span className="unit-symbol">Months</span>
                </div>
              </div>
              <input
                type="range"
                min="6"
                max="120"
                step="1"
                value={tenureMonths}
                onChange={(e) => setTenureMonths(Number(e.target.value))}
                className="calc-range-slider"
              />
              <div className="range-bounds">
                <span>6 Months</span>
                <span>{Math.floor(tenureMonths / 12)} Yrs {tenureMonths % 12} Mos</span>
                <span>120 Months</span>
              </div>
            </div>
          </div>

          {/* Results Column */}
          <div className="calc-results-col">
            <div className="results-card-inner">
              <span className="result-label">Estimated Monthly EMI</span>
              <div className="result-emi-amount">{formatCurrency(monthlyEmi)}</div>
              <span className="result-emi-subtext">per month for {tenureMonths} months</span>

              <div className="breakdown-bar-container">
                <div className="breakdown-bar">
                  <div 
                    className="bar-principal" 
                    style={{ width: `${100 - interestRatio}%` }}
                    title={`Principal: ${100 - interestRatio}%`}
                  ></div>
                  <div 
                    className="bar-interest" 
                    style={{ width: `${interestRatio}%` }}
                    title={`Interest: ${interestRatio}%`}
                  ></div>
                </div>
                <div className="bar-legend">
                  <div className="legend-item">
                    <span className="legend-dot dot-principal"></span>
                    <span>Principal ({100 - interestRatio}%)</span>
                  </div>
                  <div className="legend-item">
                    <span className="legend-dot dot-interest"></span>
                    <span>Interest ({interestRatio}%)</span>
                  </div>
                </div>
              </div>

              <div className="summary-list">
                <div className="summary-row">
                  <span>Principal Amount</span>
                  <strong>{formatCurrency(amount)}</strong>
                </div>
                <div className="summary-row">
                  <span>Total Interest Payable</span>
                  <strong className="text-orange">{formatCurrency(totalInterest)}</strong>
                </div>
                <div className="summary-row summary-total">
                  <span>Total Amount Payable</span>
                  <strong>{formatCurrency(totalPayment)}</strong>
                </div>
              </div>

              <a href={applyUrl} className="btn calc-apply-btn">
                Apply for this Loan →
              </a>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .calculator-section {
          padding: 80px 24px;
          background: #FFFFFF;
        }

        .calculator-container {
          max-width: 1160px;
          margin: 0 auto;
        }

        .section-header {
          text-align: center;
          margin-bottom: 48px;
        }

        .section-pill {
          display: inline-block;
          background: #EFF6FF;
          color: #1D4ED8;
          font-size: 12px;
          font-weight: 700;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          padding: 5px 14px;
          border-radius: 20px;
          margin-bottom: 12px;
        }

        .section-title {
          font-size: 36px;
          font-weight: 800;
          color: var(--dark-navy);
          margin: 0 0 12px;
        }

        .section-subtitle {
          color: var(--text-secondary);
          font-size: 16px;
          max-width: 620px;
          margin: 0 auto;
          line-height: 1.6;
        }

        .calculator-card {
          display: grid;
          grid-template-columns: 1.2fr 0.9fr;
          gap: 36px;
          background: #FFFFFF;
          border: 1px solid var(--border-light);
          border-radius: 16px;
          padding: 40px;
          box-shadow: var(--shadow-md);
        }

        .calc-control-group {
          margin-bottom: 32px;
        }

        .control-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;
        }

        .control-header label {
          font-size: 15px;
          font-weight: 700;
          color: var(--dark-navy);
        }

        .display-box {
          display: flex;
          align-items: center;
          background: #F8FAFC;
          border: 1.5px solid var(--border-subtle);
          border-radius: 8px;
          padding: 6px 12px;
        }

        .calc-num-input {
          border: none;
          background: transparent;
          font-size: 16px;
          font-weight: 700;
          color: var(--dark-navy);
          width: 120px;
          text-align: right;
          outline: none;
          padding: 0;
          margin: 0;
        }

        .currency-symbol, .unit-symbol {
          font-weight: 700;
          color: var(--text-muted);
          font-size: 14px;
        }

        .calc-range-slider {
          width: 100%;
          height: 8px;
          border-radius: 4px;
          background: #E2E8F0;
          outline: none;
          -webkit-appearance: none;
          cursor: pointer;
          accent-color: var(--accent-orange);
          margin: 10px 0;
          padding: 0;
        }

        .range-bounds {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: var(--text-muted);
        }

        .calc-results-col {
          display: flex;
        }

        .results-card-inner {
          background: #F8FAFC;
          border: 1px solid var(--border-light);
          border-radius: 12px;
          padding: 32px 28px;
          width: 100%;
          display: flex;
          flex-direction: column;
          justify-content: space-between;
        }

        .result-label {
          font-size: 14px;
          font-weight: 600;
          color: var(--text-muted);
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }

        .result-emi-amount {
          font-size: 40px;
          font-weight: 800;
          color: var(--accent-orange);
          margin: 6px 0;
        }

        .result-emi-subtext {
          font-size: 13px;
          color: var(--text-muted);
          margin-bottom: 24px;
          display: block;
        }

        .breakdown-bar-container {
          margin-bottom: 24px;
        }

        .breakdown-bar {
          height: 10px;
          border-radius: 5px;
          background: #E2E8F0;
          display: flex;
          overflow: hidden;
          margin-bottom: 8px;
        }

        .bar-principal {
          background: var(--primary-navy);
          transition: width 0.3s ease;
        }

        .bar-interest {
          background: var(--accent-orange);
          transition: width 0.3s ease;
        }

        .bar-legend {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: var(--text-secondary);
        }

        .legend-item {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .legend-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
        }

        .dot-principal {
          background: var(--primary-navy);
        }

        .dot-interest {
          background: var(--accent-orange);
        }

        .summary-list {
          border-top: 1px solid var(--border-light);
          padding-top: 16px;
          margin-bottom: 24px;
        }

        .summary-row {
          display: flex;
          justify-content: space-between;
          padding: 8px 0;
          font-size: 14px;
          color: var(--text-secondary);
        }

        .summary-row strong {
          color: var(--dark-navy);
        }

        .text-orange {
          color: var(--accent-orange) !important;
        }

        .summary-total {
          border-top: 1.5px dashed var(--border-subtle);
          padding-top: 12px;
          margin-top: 6px;
          font-size: 15px;
          font-weight: 700;
          color: var(--dark-navy);
        }

        .calc-apply-btn {
          width: 100%;
          padding: 14px;
          font-size: 15px;
          border-radius: 8px;
          text-align: center;
        }

        @media (max-width: 868px) {
          .calculator-card {
            grid-template-columns: 1fr;
            padding: 24px;
          }
        }
      `}</style>
    </section>
  );
}
