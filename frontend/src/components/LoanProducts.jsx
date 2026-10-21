import React, { useState } from 'react';
import { Link } from 'react-router-dom';

const products = [
  {
    id: 'personal',
    name: 'Personal Express Loan',
    icon: '💳',
    category: 'personal',
    rate: '10.5% p.a.',
    tenure: 'Up to 24 Months',
    maxAmount: '₹15,00,000',
    desc: 'Instant digital credit for medical emergencies, travel, weddings, or personal milestones.',
    features: ['100% Paperless Application', 'No Collateral or Guarantor', 'Disbursal in 15 Minutes', 'Zero Foreclosure Fee']
  },
  {
    id: 'business',
    name: 'MSME Business Growth',
    icon: '🏢',
    category: 'business',
    rate: '9.8% p.a.',
    tenure: 'Up to 48 Months',
    maxAmount: '₹1,00,00,000',
    desc: 'Working capital and machinery financing tailored to fuel fast-growing Indian enterprises.',
    features: ['Quick In-Principle Approval', 'Flexible Repayment Holiday', 'Tax Advantage Benefits', 'Dedicated Loan Officer']
  },
  {
    id: 'home',
    name: 'Dream Home Loan',
    icon: '🏡',
    category: 'property',
    rate: '8.4% p.a.',
    tenure: 'Up to 30 Years',
    maxAmount: '₹5,00,00,000',
    desc: 'Lowest interest rates to buy, construct, or renovate your dream residential property.',
    features: ['Pradhan Mantri Awas Yojana Subsidy', 'Complimentary Property Valuation', 'Step-Up Repayment Options', 'Pre-Approved Top-up Loans']
  },
  {
    id: 'education',
    name: 'Global Scholar Education',
    icon: '🎓',
    category: 'personal',
    rate: '9.2% p.a.',
    tenure: 'Up to 15 Years',
    maxAmount: '₹75,00,000',
    desc: 'Finance premier higher education in India and top foreign universities worldwide.',
    features: ['Covers Tuition, Living & Travel', 'Moratorium Period (Course + 1 Yr)', 'Section 80E Tax Deduction', 'Zero Margin up to ₹7.5 Lakhs']
  },
  {
    id: 'vehicle',
    name: 'Auto & EV Green Credit',
    icon: '⚡',
    category: 'vehicle',
    rate: '8.7% p.a.',
    tenure: 'Up to 7 Years',
    maxAmount: '₹40,00,000',
    desc: 'Special green discount interest rates on Electric Vehicles and passenger four-wheelers.',
    features: ['Up to 100% On-Road Funding', 'Instant Dealer Disbursal', 'EV Green Subsidy Eligible', 'Free Roadside Assistance']
  },
  {
    id: 'gold',
    name: 'Instant Gold Loan',
    icon: '🪙',
    category: 'personal',
    rate: '8.9% p.a.',
    tenure: 'Up to 12 Months',
    maxAmount: '₹50,00,000',
    desc: 'Unlock the value of your gold jewelry with safe custody in Indian Bank fireproof vaults.',
    features: ['Instant 5-Minute Over-the-Counter', 'Bullet Repayment Option', 'Highest Per-Gram Valuation', 'Zero Appraisal Charges']
  }
];

export default function LoanProducts({ backendUrl }) {
  const [activeCategory, setActiveCategory] = useState('all');

  const filtered = activeCategory === 'all' 
    ? products 
    : products.filter(p => p.category === activeCategory);

  return (
    <section className="section-wrapper" id="loans">
      <div className="container">
        <div className="section-header">
          <span className="section-tag">Competitive Lending Portfolio</span>
          <h2 className="section-title">Designed for <span className="gradient-text">Every Financial Need</span></h2>
          <p className="section-desc">
            Explore our comprehensive suite of retail and institutional loan products with competitive interest rates and transparent conditions.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '24px', flexWrap: 'wrap' }}>
            {[
              { id: 'all', label: 'All Products' },
              { id: 'personal', label: 'Personal & Education' },
              { id: 'business', label: 'Business & MSME' },
              { id: 'property', label: 'Home & Property' },
              { id: 'vehicle', label: 'Auto & Green EV' }
            ].map(tab => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveCategory(tab.id)}
                style={{
                  background: activeCategory === tab.id ? 'var(--primary-gradient)' : 'rgba(255, 255, 255, 0.05)',
                  color: activeCategory === tab.id ? '#fff' : 'var(--text-muted)',
                  border: '1px solid ' + (activeCategory === tab.id ? 'transparent' : 'var(--border-glass)'),
                  padding: '8px 18px',
                  borderRadius: '10px',
                  fontSize: '13px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  transition: 'var(--transition-fast)'
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        <div className="products-grid">
          {filtered.map(product => (
            <div key={product.id} className="glass-panel product-card">
              <div>
                <div className="product-icon-wrap">
                  <span>{product.icon}</span>
                </div>
                <h3 className="product-name">{product.name}</h3>
                <p className="product-desc">{product.desc}</p>

                <div style={{ background: 'rgba(7, 11, 20, 0.5)', padding: '12px 14px', borderRadius: '10px', marginBottom: '18px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ fontSize: '11px', color: '#64748b', textTransform: 'uppercase' }}>Starting At</div>
                    <div style={{ fontSize: '16px', fontWeight: 800, color: '#34d399' }}>{product.rate}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '11px', color: '#64748b', textTransform: 'uppercase' }}>Max Amount</div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: '#fff' }}>{product.maxAmount}</div>
                  </div>
                </div>

                <ul className="product-feature-list">
                  {product.features.map((feat, idx) => (
                    <li key={idx}>
                      <span className="feature-check">✓</span>
                      <span>{feat}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <Link 
                to="/apply-loan"
                className="btn-outline-glow" 
                style={{ textAlign: 'center', width: '100%', display: 'block', padding: '10px 0', textDecoration: 'none' }}
              >
                Apply for {product.name.split(' ')[0]} ➔
              </Link>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
