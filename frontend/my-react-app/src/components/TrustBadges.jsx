import React from 'react';

export default function TrustBadges() {
  const securityPillars = [
    {
      icon: '🔐',
      title: 'MFA & TOTP Authenticator',
      desc: 'Industry-standard Time-based One-Time Passwords with encrypted secret storage ensure zero unauthorized access.'
    },
    {
      icon: '🛡️',
      title: 'Role-Based Access Control',
      desc: 'Enforced separation of duties across Customers, Loan Officers, Underwriters, Collections Agents, and Administrators.'
    },
    {
      icon: '🔔',
      title: 'Automated DPD Reminders',
      desc: 'Transparent reminder workflows and scheduled notifications with clear, predefined penalty caps.'
    },
    {
      icon: '📄',
      title: 'Instant PDF Statements',
      desc: 'Export digitally stamped loan account statements, repayment schedules, and interest certificates on demand.'
    }
  ];

  return (
    <section className="security-section">
      <div className="security-container">
        <div className="section-header">
          <span className="section-pill">Enterprise Banking Architecture</span>
          <h2 className="section-title">Built with Security & Compliance at the Core</h2>
          <p className="section-subtitle">
            Every transaction, calculation, and access request is guarded by multi-layered institutional safeguards.
          </p>
        </div>

        <div className="security-grid">
          {securityPillars.map((item, idx) => (
            <div key={idx} className="security-card">
              <div className="security-icon" aria-hidden="true">{item.icon}</div>
              <h3 className="security-card-title">{item.title}</h3>
              <p className="security-card-desc">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>

      <style>{`
        .security-section {
          padding: 80px 24px;
          background: #FFFFFF;
        }

        .security-container {
          max-width: 1240px;
          margin: 0 auto;
        }

        .security-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
          gap: 28px;
        }

        .security-card {
          background: #F8FAFC;
          border: 1px solid var(--border-light);
          border-radius: 12px;
          padding: 30px 24px;
          transition: transform 0.2s ease, border-color 0.2s ease;
        }

        .security-card:hover {
          transform: translateY(-2px);
          border-color: var(--primary-navy);
        }

        .security-icon {
          font-size: 32px;
          margin-bottom: 16px;
        }

        .security-card-title {
          font-size: 18px;
          font-weight: 700;
          color: var(--dark-navy);
          margin: 0 0 10px;
        }

        .security-card-desc {
          font-size: 14px;
          color: var(--text-secondary);
          line-height: 1.55;
          margin: 0;
        }
      `}</style>
    </section>
  );
}
