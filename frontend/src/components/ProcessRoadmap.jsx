import React from 'react';

const steps = [
  {
    num: '01',
    icon: '📝',
    title: 'Digital Application',
    desc: 'Enter basic KYC details and loan requirement in under 2 minutes with Aadhaar / PAN e-sign.'
  },
  {
    num: '02',
    icon: '⚡',
    title: 'Smart Underwriting',
    desc: 'Our automated algorithms verify credit scores and pre-qualify your terms at 10.5% standard fixed rate.'
  },
  {
    num: '03',
    icon: '🛡️',
    title: 'Fast Officer Review',
    desc: 'Authorized Indian Bank credit managers approve your application with zero branch visits needed.'
  },
  {
    num: '04',
    icon: '💸',
    title: 'Direct Disbursal',
    desc: 'Approved loan proceeds are transferred directly into your Indian Bank auto-debit account in 15 minutes.'
  }
];

export default function ProcessRoadmap() {
  return (
    <section className="section-wrapper" id="process" style={{ background: 'rgba(7, 11, 20, 0.4)' }}>
      <div className="container">
        <div className="section-header">
          <span className="section-tag">Seamless 4-Step Process</span>
          <h2 className="section-title">How Digital Lending <span className="gradient-text">Works</span></h2>
          <p className="section-desc">
            No long queues, no endless paperwork. From application to money in your account in 4 clear steps.
          </p>
        </div>

        <div className="process-grid">
          {steps.map(step => (
            <div key={step.num} className="glass-panel process-step-card">
              <span className="step-number">{step.num}</span>
              <span className="step-icon">{step.icon}</span>
              <h3 className="step-title">{step.title}</h3>
              <p className="step-desc">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
