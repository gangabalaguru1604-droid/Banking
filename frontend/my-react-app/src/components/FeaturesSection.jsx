import React from 'react';

export default function FeaturesSection({ authState }) {
  const { authenticated } = authState;
  const applyUrl = authenticated ? '/applyLoan' : '/register';

  const products = [
    {
      id: 'personal',
      title: 'Personal Loan',
      rate: 'From 10.5% p.a.',
      icon: '💼',
      features: [
        'Instant digital eligibility check',
        'Minimal paperwork & fast sanction',
        'Tenure from 12 to 60 months',
        'No end-use restrictions'
      ],
      tag: 'Most Popular'
    },
    {
      id: 'home',
      title: 'Home Loan',
      rate: 'From 8.4% p.a.',
      icon: '🏡',
      features: [
        'High value loan financing up to ₹1 Crore',
        'Extended repayment tenures up to 20 years',
        'Special concessional rates for women',
        'Easy balance transfer facilities'
      ],
      tag: 'Best Rates'
    },
    {
      id: 'education',
      title: 'Education Loan',
      rate: 'From 9.2% p.a.',
      icon: '🎓',
      features: [
        'Comprehensive study cost coverage',
        'Flexible moratorium during course study',
        'Repay after course completion + 1 year',
        'Tax savings under Section 80E'
      ],
      tag: 'Student Friendly'
    },
    {
      id: 'vehicle',
      title: 'Vehicle Loan',
      rate: 'From 9.8% p.a.',
      icon: '🚗',
      features: [
        'Up to 90% on-road financing',
        'Fixed interest rates across tenure',
        'Quick processing within 24 hours',
        'Zero prepayment penalties after 12 months'
      ],
      tag: 'Quick Disbursal'
    }
  ];

  return (
    <section id="features" className="features-section">
      <div className="features-container">
        <div className="section-header">
          <span className="section-pill">Our Products</span>
          <h2 className="section-title">Designed for Every Life Milestone</h2>
          <p className="section-subtitle">
            Choose from custom-tailored credit solutions engineered with competitive rates, structured repayment cycles, and complete transparency.
          </p>
        </div>

        <div className="products-grid">
          {products.map((prod) => (
            <div key={prod.id} className="product-card">
              {prod.tag && <span className="product-tag">{prod.tag}</span>}
              <div className="product-icon-wrap" aria-hidden="true">{prod.icon}</div>
              <h3 className="product-title">{prod.title}</h3>
              <div className="product-rate">{prod.rate}</div>

              <ul className="product-features">
                {prod.features.map((feat, idx) => (
                  <li key={idx}>
                    <span className="check-icon" aria-hidden="true">✓</span>
                    <span>{feat}</span>
                  </li>
                ))}
              </ul>

              <a href={applyUrl} className="product-action-btn">
                Apply Now →
              </a>
            </div>
          ))}
        </div>
      </div>

      <style>{`
        .features-section {
          padding: 80px 24px;
          background: #F8FAFC;
          border-top: 1px solid var(--border-light);
          border-bottom: 1px solid var(--border-light);
        }

        .features-container {
          max-width: 1240px;
          margin: 0 auto;
        }

        .products-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
          gap: 28px;
        }

        .product-card {
          background: #FFFFFF;
          border: 1px solid var(--border-light);
          border-radius: 14px;
          padding: 32px 24px;
          position: relative;
          display: flex;
          flex-direction: column;
          transition: transform 0.2s ease, box-shadow 0.2s ease;
          box-shadow: var(--shadow-sm);
        }

        .product-card:hover {
          transform: translateY(-4px);
          box-shadow: var(--shadow-lg);
          border-color: var(--accent-orange);
        }

        .product-tag {
          position: absolute;
          top: 18px;
          right: 18px;
          background: #FEF3C7;
          color: #92400E;
          font-size: 11px;
          font-weight: 700;
          padding: 4px 10px;
          border-radius: 12px;
          text-transform: uppercase;
          letter-spacing: 0.3px;
        }

        .product-icon-wrap {
          font-size: 36px;
          margin-bottom: 16px;
          width: 56px;
          height: 56px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #F1F5F9;
          border-radius: 12px;
        }

        .product-title {
          font-size: 20px;
          font-weight: 800;
          color: var(--dark-navy);
          margin: 0 0 6px;
        }

        .product-rate {
          font-size: 14px;
          font-weight: 700;
          color: var(--accent-orange);
          margin-bottom: 20px;
        }

        .product-features {
          list-style: none;
          padding: 0;
          margin: 0 0 28px;
          flex-grow: 1;
        }

        .product-features li {
          display: flex;
          align-items: flex-start;
          gap: 10px;
          font-size: 13px;
          color: var(--text-secondary);
          margin-bottom: 10px;
          line-height: 1.4;
        }

        .check-icon {
          color: var(--success-green);
          font-weight: 800;
          font-size: 14px;
        }

        .product-action-btn {
          display: block;
          text-align: center;
          text-decoration: none;
          background: #F1F5F9;
          color: var(--primary-navy);
          font-size: 14px;
          font-weight: 700;
          padding: 10px 18px;
          border-radius: 6px;
          transition: all 0.15s ease-in-out;
        }

        .product-card:hover .product-action-btn {
          background: var(--accent-orange);
          color: #FFFFFF;
        }
      `}</style>
    </section>
  );
}
