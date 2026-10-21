import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { initialLoans } from '../services/mockData';

export default function LoanAccountDetailsPage() {
  const { id } = useParams();
  const loanId = Number(id || 101);
  const loan = initialLoans.find(l => l.id === loanId) || initialLoans[0];

  const formatINR = (val) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);

  // Generate 24 amortization schedule installments
  const schedule = Array.from({ length: loan.tenureMonths }, (_, i) => {
    const instNum = i + 1;
    const isPaid = instNum <= loan.installmentsPaid;
    const isOverdue = instNum === loan.installmentsPaid + 1 && loan.dpdDays > 0;
    const d = new Date(2026, 8 + i, 15);
    const dateStr = d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });

    const interestPart = Math.round(loan.monthlyEmi * 0.35);
    const principalPart = loan.monthlyEmi - interestPart;

    return {
      num: instNum,
      dueDate: dateStr,
      principal: principalPart,
      interest: interestPart,
      emi: loan.monthlyEmi,
      status: isPaid ? 'PAID' : isOverdue ? 'OVERDUE' : 'UPCOMING',
      txnId: isPaid ? `TXN-IB-${1000 + instNum}` : '-'
    };
  });

  return (
    <div className="container" style={{ padding: '40px 20px 80px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#fff' }}>
              Loan {loan.loanNumber}
            </h1>
            <span style={{
              background: loan.status === 'ACTIVE' ? 'rgba(16,185,129,0.2)' : loan.status === 'OVERDUE' ? 'rgba(239,68,68,0.2)' : 'rgba(59,130,246,0.2)',
              color: loan.status === 'ACTIVE' ? '#34d399' : loan.status === 'OVERDUE' ? '#f87171' : '#60a5fa',
              padding: '4px 12px',
              borderRadius: '8px',
              fontSize: '12px',
              fontWeight: 800
            }}>
              {loan.status} {loan.dpdDays > 0 ? `(${loan.dpdDays} DPD)` : ''}
            </span>
          </div>
          <p style={{ color: '#94a3b8', fontSize: '14px', marginTop: '4px' }}>
            Borrower: <strong>{loan.customerName}</strong> ({loan.customerEmail}) • Purpose: {loan.purpose}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <Link to={`/pay?loanId=${loan.id}`} className="btn-primary" style={{ padding: '8px 18px', fontSize: '13px' }}>
            Make Repayment
          </Link>
          <Link to="/dashboard" className="btn-secondary" style={{ padding: '8px 16px', fontSize: '13px' }}>
            Back to Dashboard
          </Link>
        </div>
      </div>

      {/* Top 3 Metric Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px', marginBottom: '32px' }}>
        <div className="glass-panel" style={{ padding: '24px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Loan Principal & APR</div>
          <div style={{ fontFamily: 'var(--font-heading)', fontSize: '26px', fontWeight: 800, color: '#60a5fa', marginTop: '6px' }}>
            {formatINR(loan.amount)}
          </div>
          <div style={{ fontSize: '13px', color: '#cbd5e1', marginTop: '4px' }}>
            Fixed Rate: <strong>{loan.interestRate}% p.a.</strong> • Tenure: <strong>{loan.tenureMonths} Months</strong>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '24px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Total Repaid vs Outstanding</div>
          <div style={{ fontFamily: 'var(--font-heading)', fontSize: '26px', fontWeight: 800, color: '#34d399', marginTop: '6px' }}>
            {formatINR(loan.totalPaid)}
          </div>
          <div style={{ fontSize: '13px', color: '#f59e0b', marginTop: '4px' }}>
            Remaining Balance: <strong>{formatINR(loan.remainingBalance)}</strong>
          </div>
        </div>

        {/* Linked Auto-Debit Account */}
        <div className="glass-panel" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Linked Auto-Debit Account</span>
            <span style={{ fontSize: '11px', color: '#34d399', fontWeight: 700 }}>MANDATE ACTIVE</span>
          </div>
          <div style={{ fontFamily: 'var(--font-heading)', fontSize: '20px', fontWeight: 700, color: '#fff', marginTop: '8px' }}>
            {loan.linkedAccount.accountNumber}
          </div>
          <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>
            {loan.linkedAccount.bankName} • IFSC: <code>{loan.linkedAccount.ifscCode}</code>
          </div>
          <div style={{ fontSize: '12px', color: '#34d399', marginTop: '6px', fontWeight: 600 }}>
            🟢 Live CBS Balance: {formatINR(loan.linkedAccount.liveBalance)}
          </div>
        </div>
      </div>

      {/* Amortization Schedule Table */}
      <div className="glass-panel" style={{ padding: '30px' }}>
        <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '20px' }}>
          Amortization & Installment Repayment Schedule
        </h3>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>
                <th style={{ padding: '12px 16px' }}>Inst #</th>
                <th style={{ padding: '12px 16px' }}>Due Date</th>
                <th style={{ padding: '12px 16px' }}>Principal</th>
                <th style={{ padding: '12px 16px' }}>Interest</th>
                <th style={{ padding: '12px 16px' }}>Total EMI</th>
                <th style={{ padding: '12px 16px' }}>Status</th>
                <th style={{ padding: '12px 16px' }}>Transaction ID</th>
              </tr>
            </thead>
            <tbody>
              {schedule.map(item => (
                <tr key={item.num} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#cbd5e1' }}>
                  <td style={{ padding: '14px 16px', fontWeight: 700 }}>#{item.num}</td>
                  <td style={{ padding: '14px 16px' }}>{item.dueDate}</td>
                  <td style={{ padding: '14px 16px' }}>{formatINR(item.principal)}</td>
                  <td style={{ padding: '14px 16px', color: '#f59e0b' }}>{formatINR(item.interest)}</td>
                  <td style={{ padding: '14px 16px', fontWeight: 700, color: '#fff' }}>{formatINR(item.emi)}</td>
                  <td style={{ padding: '14px 16px' }}>
                    <span style={{
                      background: item.status === 'PAID' ? 'rgba(16,185,129,0.15)' : item.status === 'OVERDUE' ? 'rgba(239,68,68,0.2)' : 'rgba(255,255,255,0.06)',
                      color: item.status === 'PAID' ? '#34d399' : item.status === 'OVERDUE' ? '#f87171' : '#94a3b8',
                      padding: '3px 8px',
                      borderRadius: '6px',
                      fontSize: '11px',
                      fontWeight: 700
                    }}>
                      {item.status}
                    </span>
                  </td>
                  <td style={{ padding: '14px 16px', fontFamily: 'monospace', fontSize: '12px', color: '#60a5fa' }}>
                    {item.txnId}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
