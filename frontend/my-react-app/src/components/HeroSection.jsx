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

        {/* Right Column: Hero Visual with Digital Banking & Loan Management Illustration */}
        <div className="hero-visual">
          <div className="visual-wrapper animate-float">
            <div className="brand-hero-card">
              <svg
                viewBox="0 0 340 230"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                className="hero-illustration-svg"
                aria-label="Digital Banking & Loan Management"
                role="img"
              >
                <defs>
                  <linearGradient id="phoneGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#0050A4" />
                    <stop offset="100%" stopColor="#002B49" />
                  </linearGradient>
                  <linearGradient id="screenGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stopColor="#FFFFFF" />
                    <stop offset="100%" stopColor="#F8FAFC" />
                  </linearGradient>
                  <linearGradient id="orangeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#F97316" />
                    <stop offset="100%" stopColor="#ED7D16" />
                  </linearGradient>
                  <linearGradient id="coinGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#FBBF24" />
                    <stop offset="100%" stopColor="#F59E0B" />
                  </linearGradient>
                  <filter id="cardShadow" x="-10%" y="-10%" width="120%" height="130%" filterUnits="userSpaceOnUse">
                    <feDropShadow dx="0" dy="8" stdDeviation="10" floodColor="#0050A4" floodOpacity="0.12" />
                  </filter>
                  <filter id="floatShadow" x="-10%" y="-10%" width="120%" height="130%" filterUnits="userSpaceOnUse">
                    <feDropShadow dx="0" dy="6" stdDeviation="7" floodColor="#000000" floodOpacity="0.08" />
                  </filter>
                </defs>

                {/* Background Subtle Digital Soft Circles */}
                <circle cx="170" cy="115" r="95" fill="#EFF6FF" opacity="0.7"/>
                <circle cx="70" cy="70" r="45" fill="#FFF7ED" opacity="0.6"/>

                {/* Left: Approved Loan Document Card */}
                <g transform="translate(28, 45)" filter="url(#floatShadow)">
                  <rect width="112" height="135" rx="10" fill="#FFFFFF" stroke="#E2E8F0" strokeWidth="1.5"/>
                  {/* Document Header Bar */}
                  <rect x="12" y="14" width="45" height="6" rx="3" fill="#0050A4"/>
                  <rect x="12" y="26" width="72" height="4" rx="2" fill="#E2E8F0"/>
                  <rect x="12" y="36" width="56" height="4" rx="2" fill="#E2E8F0"/>
                  <rect x="12" y="46" width="66" height="4" rx="2" fill="#E2E8F0"/>
                  {/* Divider */}
                  <line x1="12" y1="58" x2="100" y2="58" stroke="#F1F5F9" strokeWidth="1.5"/>
                  {/* Amount Section */}
                  <rect x="12" y="68" width="36" height="4" rx="2" fill="#94A3B8"/>
                  <text x="12" y="87" fill="#002B49" fontSize="13" fontWeight="bold" fontFamily="system-ui, sans-serif">₹5,00,000</text>
                  {/* Verified Stamp / Approval Badge */}
                  <rect x="12" y="97" width="70" height="20" rx="10" fill="#ECFDF5" stroke="#A7F3D0" strokeWidth="1"/>
                  <circle cx="22" cy="107" r="5" fill="#10B981"/>
                  <path d="M19.5 107 L21.5 109 L24.5 105" stroke="#FFFFFF" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
                  <text x="32" y="111" fill="#065F46" fontSize="9" fontWeight="bold" fontFamily="system-ui, sans-serif">APPROVED</text>
                </g>

                {/* Center-Right: Smartphone with Loan Dashboard */}
                <g transform="translate(155, 15)" filter="url(#cardShadow)">
                  {/* Phone Body */}
                  <rect width="135" height="195" rx="20" fill="url(#phoneGrad)"/>
                  {/* Screen Glass */}
                  <rect x="6" y="6" width="123" height="183" rx="15" fill="url(#screenGrad)"/>
                  {/* Speaker Notch */}
                  <rect x="52" y="10" width="31" height="4" rx="2" fill="#CBD5E1"/>

                  {/* App Top Bar */}
                  <rect x="14" y="24" width="22" height="4" rx="2" fill="#0050A4"/>
                  <circle cx="115" cy="26" r="3" fill="#ED7D16"/>

                  {/* Loan Sanction Banner */}
                  <rect x="14" y="36" width="107" height="45" rx="8" fill="#0050A4"/>
                  <text x="22" y="50" fill="#93C5FD" fontSize="8" fontFamily="system-ui, sans-serif">Active Loan Sanction</text>
                  <text x="22" y="67" fill="#FFFFFF" fontSize="14" fontWeight="bold" fontFamily="system-ui, sans-serif">₹10,00,000</text>
                  <rect x="91" y="44" width="23" height="12" rx="4" fill="#ED7D16"/>
                  <text x="94" y="53" fill="#FFFFFF" fontSize="7" fontWeight="bold" fontFamily="system-ui, sans-serif">10.5%</text>

                  {/* Loan Progress Section */}
                  <rect x="14" y="90" width="107" height="48" rx="8" fill="#FFFFFF" stroke="#E2E8F0" strokeWidth="1"/>
                  <text x="22" y="103" fill="#64748B" fontSize="8" fontFamily="system-ui, sans-serif">Repayment Progress</text>
                  <text x="94" y="103" fill="#0050A4" fontSize="8" fontWeight="bold" fontFamily="system-ui, sans-serif">65%</text>
                  {/* Progress Bar Track & Active Fill */}
                  <rect x="22" y="110" width="91" height="6" rx="3" fill="#E2E8F0"/>
                  <rect x="22" y="110" width="59" height="6" rx="3" fill="url(#orangeGrad)"/>
                  <text x="22" y="128" fill="#10B981" fontSize="8" fontWeight="600" fontFamily="system-ui, sans-serif">● On Schedule (0 DPD)</text>

                  {/* Quick Action Pay Button */}
                  <rect x="14" y="146" width="107" height="26" rx="6" fill="#ED7D16"/>
                  <text x="41" y="163" fill="#FFFFFF" fontSize="10" fontWeight="bold" fontFamily="system-ui, sans-serif">Pay EMI Online</text>
                  <path d="M102 159 L106 159 M104 156 L107 159 L104 162" stroke="#FFFFFF" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
                </g>

                {/* Stacked Rupee Coins in Foreground */}
                <g transform="translate(125, 148)" filter="url(#floatShadow)">
                  {/* Bottom Coin */}
                  <ellipse cx="22" cy="38" rx="16" ry="6" fill="#D97706"/>
                  <ellipse cx="22" cy="35" rx="16" ry="6" fill="url(#coinGrad)"/>
                  {/* Middle Coin */}
                  <ellipse cx="22" cy="30" rx="16" ry="6" fill="#D97706"/>
                  <ellipse cx="22" cy="27" rx="16" ry="6" fill="url(#coinGrad)"/>
                  {/* Top Coin */}
                  <ellipse cx="22" cy="22" rx="16" ry="6" fill="#D97706"/>
                  <ellipse cx="22" cy="19" rx="16" ry="6" fill="url(#coinGrad)"/>
                  <text x="18" y="22" fill="#78350F" fontSize="10" fontWeight="bold" fontFamily="system-ui, sans-serif">₹</text>

                  {/* Secondary Small Stack */}
                  <ellipse cx="48" cy="40" rx="13" ry="5" fill="#D97706"/>
                  <ellipse cx="48" cy="37" rx="13" ry="5" fill="url(#coinGrad)"/>
                  <ellipse cx="48" cy="32" rx="13" ry="5" fill="#D97706"/>
                  <ellipse cx="48" cy="29" rx="13" ry="5" fill="url(#coinGrad)"/>
                  <text x="45" y="32" fill="#78350F" fontSize="8" fontWeight="bold" fontFamily="system-ui, sans-serif">₹</text>
                </g>

                {/* Floating Institutional Security Shield Badge */}
                <g transform="translate(265, 80)" filter="url(#floatShadow)">
                  <circle cx="20" cy="20" r="18" fill="#FFFFFF" stroke="#0050A4" strokeWidth="1.5"/>
                  <path d="M20 10 L27 13.5 V19 C27 23.5 24 27.5 20 29 C16 27.5 13 23.5 13 19 V13.5 L20 10 Z" fill="#0050A4"/>
                  <path d="M17.5 19 L19.5 21 L23 17" stroke="#FFFFFF" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
                </g>
              </svg>
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

        .hero-illustration-svg {
          width: 100%;
          max-width: 320px;
          height: auto;
          display: block;
          margin: 0 auto 16px;
          filter: drop-shadow(0 6px 18px rgba(0, 80, 164, 0.08));
          transition: transform 0.3s ease;
        }

        .hero-illustration-svg:hover {
          transform: translateY(-2px);
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
