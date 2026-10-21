import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { initialLoans } from '../services/mockData';

export default function LoanSearchPage() {
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [minAmount, setMinAmount] = useState('');
  const [maxAmount, setMaxAmount] = useState('');

  const formatINR = (val) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);

  const filtered = initialLoans.filter(loan => {
    const matchesQuery = !query || 
      loan.customerName.toLowerCase().includes(query.toLowerCase()) ||
      loan.loanNumber.toLowerCase().includes(query.toLowerCase()) ||
      loan.customerEmail.toLowerCase().includes(query.toLowerCase()) ||
      loan.purpose.toLowerCase().includes(query.toLowerCase());

    const matchesStatus = statusFilter === 'ALL' || loan.status === statusFilter;
    const matchesMin = !minAmount || loan.amount >= Number(minAmount);
    const matchesMax = !maxAmount || loan.amount <= Number(maxAmount);

    return matchesQuery && matchesStatus && matchesMin && matchesMax;
  });

  return (
    <div className="container" style={{ padding: '40px 20px 80px' }}>
      <div style={{ marginBottom: '28px' }}>
        <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#fff' }}>
          🔍 Loan Portfolio Search & Audit Filter
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '14px', marginTop: '4px' }}>
          Staff multi-parameter search engine across retail and commercial loans
        </p>
      </div>

      {/* Filter Controls Box */}
      <div className="glass-panel" style={{ padding: '24px', marginBottom: '32px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr 1fr 1fr', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
              Search Applicant / Loan #
            </label>
            <input 
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="e.g. LN-2026 or Rajesh Patil"
              style={{ width: '100%', padding: '10px 14px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
              Status Filter
            </label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ width: '100%', padding: '10px 14px', background: '#070b14', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="OVERDUE">OVERDUE (DPD Alert)</option>
              <option value="PENDING">PENDING Approval</option>
              <option value="APPROVED">APPROVED</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
              Min Amount (₹)
            </label>
            <input 
              type="number"
              value={minAmount}
              onChange={(e) => setMinAmount(e.target.value)}
              placeholder="Min"
              style={{ width: '100%', padding: '10px 14px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
              Max Amount (₹)
            </label>
            <input 
              type="number"
              value={maxAmount}
              onChange={(e) => setMaxAmount(e.target.value)}
              placeholder="Max"
              style={{ width: '100%', padding: '10px 14px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
            />
          </div>
        </div>
      </div>

      {/* Results Table */}
      <div className="glass-panel" style={{ padding: '30px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '18px', fontWeight: 700, color: '#fff' }}>
            Matching Loan Accounts ({filtered.length})
          </h3>
          <button 
            type="button" 
            onClick={() => alert('Exporting results to Excel/CSV format...')} 
            className="btn-outline-glow" 
            style={{ fontSize: '12px' }}
          >
            📥 Export Report
          </button>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>
                <th style={{ padding: '12px 16px' }}>Loan ID</th>
                <th style={{ padding: '12px 16px' }}>Customer Name</th>
                <th style={{ padding: '12px 16px' }}>Principal Amount</th>
                <th style={{ padding: '12px 16px' }}>Monthly EMI</th>
                <th style={{ padding: '12px 16px' }}>Status</th>
                <th style={{ padding: '12px 16px' }}>Purpose</th>
                <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(loan => (
                <tr key={loan.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#cbd5e1' }}>
                  <td style={{ padding: '14px 16px', fontWeight: 700 }}>
                    <Link to={`/loan-details/${loan.id}`} style={{ color: '#60a5fa', textDecoration: 'none' }}>
                      {loan.loanNumber}
                    </Link>
                  </td>
                  <td style={{ padding: '14px 16px' }}>{loan.customerName}</td>
                  <td style={{ padding: '14px 16px', fontWeight: 700, color: '#fff' }}>{formatINR(loan.amount)}</td>
                  <td style={{ padding: '14px 16px', color: '#34d399', fontWeight: 700 }}>{formatINR(loan.monthlyEmi)}</td>
                  <td style={{ padding: '14px 16px' }}>
                    <span style={{
                      background: loan.status === 'ACTIVE' ? 'rgba(16,185,129,0.15)' : loan.status === 'OVERDUE' ? 'rgba(239,68,68,0.2)' : 'rgba(59,130,246,0.2)',
                      color: loan.status === 'ACTIVE' ? '#34d399' : loan.status === 'OVERDUE' ? '#f87171' : '#60a5fa',
                      padding: '3px 8px',
                      borderRadius: '6px',
                      fontSize: '11px',
                      fontWeight: 700
                    }}>
                      {loan.status}
                    </span>
                  </td>
                  <td style={{ padding: '14px 16px' }}>{loan.purpose}</td>
                  <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                    <Link to={`/loan-details/${loan.id}`} className="btn-secondary" style={{ padding: '6px 12px', fontSize: '12px' }}>
                      Details ➔
                    </Link>
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
