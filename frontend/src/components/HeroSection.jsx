import React from 'react';

export default function HeroSection({ backendUrl, onScrollToCalc }) {
  return (
    <section className="hero-section" id="home">
      <div className="container">
        <div className="hero-grid">
          {/* Left Column: Copy & Actions */}
          <div>
            <div className="hero-badge">
              <span>⚡ 100% Digital & Paperless Loan Approvals</span>
            </div>

            <h1 className="hero-title">
              Empowering Ambitions With <span className="gradient-text">Instant Digital Loans</span>
            </h1>

            <p className="hero-subtitle">
              Experience modern banking with <strong>Indian Bank</strong>. Transparent fixed rates from <strong>10.5% p.a.</strong>, flexible 24-month tenures, zero prepayment penalties, and automated disbursals in 15 minutes.
            </p>

            <div className="hero-buttons">
              <a 
                href={`${backendUrl}/register`} 
                className="btn-primary"
                id="hero-apply-btn"
              >
                <span>🚀 Get Started Now</span>
              </a>

              <button 
                type="button"
                onClick={onScrollToCalc} 
                className="btn-secondary"
                id="hero-calc-btn"
              >
                <span>📊 Calculate EMI</span>
              </button>

              <a 
                href={`${backendUrl}/login`} 
                className="btn-outline-glow"
                id="hero-portal-btn"
              >
                Customer Portal ↗
              </a>
            </div>

            {/* Quick Metrics */}
            <div className="hero-stats-row">
              <div className="stat-item">
                <h4>10.5%</h4>
                <p>Standard Policy APR</p>
              </div>
              <div className="stat-item">
                <h4>15 Mins</h4>
                <p>Disbursal Time</p>
              </div>
              <div className="stat-item">
                <h4>₹500 Cr+</h4>
                <p>Disbursed in FY26</p>
              </div>
              <div className="stat-item">
                <h4>4.9 ★</h4>
                <p>Customer Rating</p>
              </div>
            </div>
          </div>

          {/* Right Column: Hero Visual Graphic */}
          <div>
            <div className="hero-image-wrapper">
              <img 
                src="/hero-bank.jpg" 
                alt="Indian Bank Digital Lending Vault" 
                className="hero-img"
                id="hero-bank-image"
              />
              <div className="floating-card-badge">
                <div>
                  <div style={{ fontSize: '11px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Active Product</div>
                  <div style={{ fontSize: '15px', fontWeight: 700, color: '#fff' }}>Personal & MSME Express Credit</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '11px', color: '#34d399', fontWeight: 700 }}>VERIFIED INSTANT</div>
                  <div style={{ fontSize: '14px', fontWeight: 800, color: '#60a5fa' }}>Up to ₹1.00 Cr</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
