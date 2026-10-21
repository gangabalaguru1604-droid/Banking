import React, { useState } from 'react';
import { getBackendUrl } from '../utils/apiConfig';

export default function HeroSection({ authState }) {
  const { authenticated, user } = authState;
  const [imageError, setImageError] = useState(false);

  // If user is logged in, CTA links to their relevant portal; otherwise /register
  const primaryCtaPath = authenticated 
    ? (user?.role === 'ROLE_CUSTOMER' ? '/applyLoan' : '/dashboard')
    : '/register';
  const primaryCtaUrl = getBackendUrl(primaryCtaPath);

  const primaryCtaLabel = authenticated
    ? (user?.role === 'ROLE_CUSTOMER' ? 'Apply for a Loan' : 'Go to Dashboard')
    : 'Get Started';

  return (
    <section className="hero-section">
      <div className="hero-container">
        {/* Left Column: Headline and Call-to-Actions */}
        <div className="hero-content animate-slide-up">
          <div className="hero-badge">
            <span className="badge-dot"></span>
            Bank of India • Official Digital Loan Portal
          </div>

          <h1 className="hero-title">
            Smart Loan <span className="highlight-text">Management</span>
          </h1>

          <p className="hero-subtitle">
            Apply loans, calculate EMI, and track payments easily with Bank of India. Experience institutional-grade security, instant pre-approvals, and complete repayment clarity.
          </p>

          <div className="hero-actions">
            <a href={primaryCtaUrl} className="btn hero-cta-btn">
              {primaryCtaLabel}
              <span className="cta-arrow" aria-hidden="true">→</span>
            </a>

            <a 
              href="#calculator" 
              className="btn btn-secondary hero-secondary-btn"
              onClick={(e) => {
                e.preventDefault();
                const el = document.getElementById('calculator');
                if (el) el.scrollIntoView({ behavior: 'smooth' });
              }}
            >
              📊 Calculate EMI
            </a>
          </div>

          {/* Quick trust metrics */}
          <div className="hero-trust-bar">
            <div className="trust-item">
              <span className="trust-icon" aria-hidden="true">🛡️</span>
              <div>
                <strong>Bank-Grade</strong>
                <span>256-bit AES & 2FA</span>
              </div>
            </div>
            <div className="trust-item">
              <span className="trust-icon" aria-hidden="true">⚡</span>
              <div>
                <strong>Fast Approvals</strong>
                <span>Zero Paper Hassles</span>
              </div>
            </div>
            <div className="trust-item">
              <span className="trust-icon" aria-hidden="true">📈</span>
              <div>
                <strong>From 10.5% p.a.</strong>
                <span>Predefined Terms</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Hero Visual with Bank of India Logo */}
        <div className="hero-visual">
          <div className="visual-wrapper animate-float">
            <div className="brand-hero-card">
              <img
                src="/images/bank-logo.jpg"
                alt="Bank of India Brand Identity"
                className="brand-card-logo"
              />
              <div className="brand-card-footer">
                <div className="brand-card-badge">
                  <span className="live-dot"></span>
                  <span>Trusted Banking Since 1906</span>
                </div>
                <p className="brand-card-caption">Smart Digital Lending & Repayment Engine</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .hero-section {
          background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%);
          padding: 60px 24px 80px;
          min-height: 72vh;
          display: flex;
          align-items: center;
          border-bottom: 1px solid var(--border-light);
        }

        .hero-container {
          max-width: 1240px;
          margin: 0 auto;
          display: grid;
          grid-template-columns: 1.15fr 0.85fr;
          gap: 48px;
          align-items: center;
          width: 100%;
        }

        .hero-badge {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          background: #FEF3C7;
          color: #92400E;
          font-size: 13px;
          font-weight: 700;
          padding: 6px 14px;
          border-radius: 20px;
          margin-bottom: 18px;
          border: 1px solid #FDE68A;
        }

        .badge-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #D97706;
          box-shadow: 0 0 6px #D97706;
        }

        .hero-title {
          font-size: 50px;
          font-weight: 800;
          color: var(--dark-navy);
          margin: 0 0 18px;
          letter-spacing: -0.8px;
          line-height: 1.15;
        }

        .highlight-text {
          color: var(--accent-orange);
          position: relative;
        }

        .hero-subtitle {
          color: var(--text-secondary);
          font-size: 18px;
          line-height: 1.65;
          margin: 0 0 32px;
          max-width: 540px;
        }

        .hero-actions {
          display: flex;
          gap: 16px;
          align-items: center;
          flex-wrap: wrap;
          margin-bottom: 40px;
        }

        .hero-cta-btn {
          padding: 14px 28px;
          font-size: 16px;
          border-radius: 8px;
          gap: 8px;
        }

        .cta-arrow {
          font-size: 18px;
          transition: transform 0.2s ease;
        }

        .hero-cta-btn:hover .cta-arrow {
          transform: translateX(4px);
        }

        .hero-secondary-btn {
          padding: 14px 24px;
          font-size: 15px;
          border-radius: 8px;
        }

        .hero-trust-bar {
          display: flex;
          gap: 24px;
          padding-top: 24px;
          border-top: 1px solid var(--border-light);
          flex-wrap: wrap;
        }

        .trust-item {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .trust-icon {
          font-size: 24px;
        }

        .trust-item strong {
          display: block;
          font-size: 13px;
          color: var(--dark-navy);
        }

        .trust-item span {
          display: block;
          font-size: 12px;
          color: var(--text-muted);
        }

        .hero-visual {
          display: flex;
          justify-content: center;
          align-items: center;
        }

        .visual-wrapper {
          position: relative;
          max-width: 480px;
          width: 100%;
        }

        .brand-hero-card {
          background: #FFFFFF;
          border: 1px solid var(--border-light);
          border-radius: 20px;
          padding: 36px 32px 24px;
          box-shadow: 0 20px 45px rgba(0, 80, 164, 0.12);
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          position: relative;
        }

        .brand-card-logo {
          max-width: 320px;
          width: 100%;
          height: auto;
          object-fit: contain;
          margin-bottom: 20px;
        }

        .brand-card-footer {
          border-top: 1px solid var(--border-light);
          padding-top: 16px;
          width: 100%;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 6px;
        }

        .brand-card-badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          background: #EFF6FF;
          color: #0050A4;
          font-size: 12px;
          font-weight: 700;
          padding: 4px 12px;
          border-radius: 12px;
        }

        .live-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: #E31B23;
        }

        .brand-card-caption {
          font-size: 13px;
          color: var(--text-muted);
          margin: 0;
        }

        @media (max-width: 960px) {
          .hero-container {
            grid-template-columns: 1fr;
            text-align: center;
            gap: 40px;
          }

          .hero-subtitle {
            margin-left: auto;
            margin-right: auto;
          }

          .hero-actions {
            justify-content: center;
          }

          .hero-trust-bar {
            justify-content: center;
          }

          .hero-title {
            font-size: 38px;
          }

          .visual-wrapper {
            max-width: 380px;
          }
        }

        @media (max-width: 480px) {
          .hero-title {
            font-size: 32px;
          }

          .hero-cta-btn, .hero-secondary-btn {
            width: 100%;
          }
        }
      `}</style>
    </section>
  );
}
