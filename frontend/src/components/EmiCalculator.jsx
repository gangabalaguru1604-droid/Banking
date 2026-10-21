import React, { useState, useMemo } from 'react';

export default function EmiCalculator({ backendUrl }) {
  const [amount, setAmount] = useState(250000);
  const [rate, setRate] = useState(10.5);
  const [tenure, setTenure] = useState(24);

  const calculation = useMemo(() => {
    const principal = Number(amount);
    const annualRate = Number(rate);
    const months = Number(tenure);

    if (principal <= 0 || annualRate <= 0 || months <= 0) {
      return { emi: 0, totalInterest: 0, totalAmount: 0, principalRatio: 100, interestRatio: 0 };
    }

    const monthlyRate = annualRate / (12 * 100);
    const numerator = principal * monthlyRate * Math.pow(1 + monthlyRate, months);
    const denominator = Math.pow(1 + monthlyRate, months) - 1;
    const emi = Math.round(numerator / denominator);
    const totalAmount = emi * months;
    const totalInterest = Math.max(0, totalAmount - principal);
    const principalRatio = Math.round((principal / totalAmount) * 100);
    const interestRatio = 100 - principalRatio;

    return { emi, totalInterest, totalAmount, principalRatio, interestRatio };
  }, [amount, rate, tenure]);

  const formatINR = (val) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);

  return (
    <section className="section-wrapper" id="calculator">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">Interactive Financial Tool</span>
          <h2 className="section-title">Smart Loan <span className="gradient-text">EMI Calculator</span></h2>
          <p className="section-desc">
            Plan your repayments with precision. Adjust loan amount, rate, and tenure to visualize your monthly breakdown with Indian Bank standard policies.
          </p>
        </div>

        <div className="calculator-card">
          <div>
            <div className="control-group">
              <div className="control-header">
                <span className="control-label">Loan Required Amount</span>
                <span className="control-value">{formatINR(amount)}</span>
              </div>
              <input 
                type="range" 
                min="10000" 
                max="5000000" 
                step="10000" 
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className="slider-input"
              />
              <div className="slider-scale">
                <span>₹10,000</span>
                <span>₹25 Lakhs</span>
                <span>₹50 Lakhs</span>
              </div>
            </div>

            <div className="control-group">
              <div className="control-header">
                <span className="control-label">Annual Interest Rate (% p.a.)</span>
                <span className="control-value">{rate}%</span>
              </div>
              <input 
                type="range" 
                min="8.0" 
                max="18.0" 
                step="0.1" 
                value={rate}
                onChange={(e) => setRate(Number(e.target.value))}
                className="slider-input"
              />
              <div className="slider-scale">
                <span>8.0%</span>
                <span style={{ color: '#60a5fa', fontWeight: 700 }}>10.5% (Bank Standard)</span>
                <span>18.0%</span>
              </div>
            </div>

            <div className="control-group">
              <div className="control-header">
                <span className="control-label">Repayment Tenure (Months)</span>
                <span className="control-value">{tenure} Months ({Math.round(tenure / 12 * 10) / 10} Yrs)</span>
              </div>
              <input 
                type="range" 
                min="6" 
                max="60" 
                step="6" 
                value={tenure}
                onChange={(e) => setTenure(Number(e.target.value))}
                className="slider-input"
              />
              <div className="slider-scale">
                <span>6 Mos</span>
                <span style={{ color: '#60a5fa', fontWeight: 700 }}>24 Mos (Standard)</span>
                <span>60 Mos</span>
              </div>
            </div>

            <div style={{ background: 'rgba(59, 130, 246, 0.08)', border: '1px solid rgba(59, 130, 246, 0.2)', padding: '12px 16px', borderRadius: '12px', fontSize: '13px', color: '#93c5fd' }}>
              ℹ️ Standard Indian Bank policy sets defaults at <strong>10.5% fixed interest</strong> and <strong>24 months tenure</strong> with zero pre-closure charges.
            </div>
          </div>

          <div className="calc-results-box">
            <div>
              <div className="emi-highlight-box">
                <div className="emi-label">Estimated Monthly EMI</div>
                <div className="emi-amount">{formatINR(calculation.emi)}</div>
                <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>Payable each month for {tenure} months</div>
              </div>

              <div style={{ marginTop: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#94a3b8', marginBottom: '6px' }}>
                  <span>Repayment Distribution</span>
                  <span>{calculation.principalRatio}% Principal / {calculation.interestRatio}% Interest</span>
                </div>
                <div className="progress-bar-container">
                  <div style={{ width: `${calculation.principalRatio}%`, height: '100%', background: '#3b82f6' }} />
                  <div style={{ width: `${calculation.interestRatio}%`, height: '100%', background: '#f59e0b' }} />
                </div>
                <div className="progress-legend">
                  <span><span className="legend-dot" style={{ background: '#3b82f6' }} />Principal ({calculation.principalRatio}%)</span>
                  <span><span className="legend-dot" style={{ background: '#f59e0b' }} />Interest ({calculation.interestRatio}%)</span>
                </div>
              </div>

              <div className="breakdown-row">
                <span className="breakdown-label">Principal Borrowed</span>
                <span className="breakdown-val">{formatINR(amount)}</span>
              </div>
              <div className="breakdown-row">
                <span className="breakdown-label">Total Interest Due</span>
                <span className="breakdown-val" style={{ color: '#f59e0b' }}>{formatINR(calculation.totalInterest)}</span>
              </div>
              <div className="breakdown-row">
                <span className="breakdown-label">Total Amount Payable</span>
                <span className="breakdown-val" style={{ color: '#60a5fa', fontSize: '16px' }}>{formatINR(calculation.totalAmount)}</span>
              </div>
            </div>

            <a 
              href="/apply-loan"
              className="btn-primary" 
              style={{ width: '100%', marginTop: '24px' }}
            >
              Apply With These Terms ➔
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
