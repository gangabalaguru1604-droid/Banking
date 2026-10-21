import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { initialLoans } from '../services/mockData';

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loans, setLoans] = useState(initialLoans);
  const [activeTab, setActiveTab] = useState('all');
  const [selectedLoan, setSelectedLoan] = useState(null);
  const [actionModal, setActionModal] = useState(null); // 'approve', 'reject', 'recordCollection'
  const [actionReason, setActionReason] = useState('');
  const [collectionAmount, setCollectionAmount] = useState('');

  // Default to customer role if not set
  const role = user?.role || 'ROLE_CUSTOMER';
  const isCustomer = role === 'ROLE_CUSTOMER';
  const isOfficerOrAdmin = role === 'ROLE_LOAN_OFFICER' || role === 'ROLE_ADMIN';
  const isCollectionsStaff = role === 'ROLE_COLLECTIONS_AGENT' || role === 'ROLE_COLLECTIONS_MANAGER' || role === 'ROLE_ADMIN';

  // Filtered loans
  const customerLoans = loans.filter(l => l.customerName === (user?.name || 'Rajesh Patil') || isCustomer);
  const pendingLoans = loans.filter(l => l.status === 'PENDING');
  const overdueLoans = loans.filter(l => l.status === 'OVERDUE');

  const formatINR = (val) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);

  const handleApprove = (loanId) => {
    setLoans(prev => prev.map(l => l.id === loanId ? { ...l, status: 'APPROVED', approvedDate: '2026-10-21' } : l));
    setActionModal(null);
    alert(`Loan LN-2026-00${loanId} approved successfully!`);
  };

  const handleReject = (loanId) => {
    if (!actionReason) {
      alert('Please enter a rejection reason.');
      return;
    }
    setLoans(prev => prev.map(l => l.id === loanId ? { ...l, status: 'REJECTED' } : l));
    setActionModal(null);
    setActionReason('');
    alert(`Loan LN-2026-00${loanId} rejected.`);
  };

  const handleRecordCollection = (loanId) => {
    if (!collectionAmount || Number(collectionAmount) <= 0) {
      alert('Please enter a valid payment collection amount.');
      return;
    }
    setLoans(prev => prev.map(l => {
      if (l.id === loanId) {
        return {
          ...l,
          status: 'ACTIVE',
          dpdDays: 0,
          penalty: 0,
          remainingBalance: Math.max(0, l.remainingBalance - Number(collectionAmount)),
          totalPaid: l.totalPaid + Number(collectionAmount),
          installmentsPaid: l.installmentsPaid + 1
        };
      }
      return l;
    }));
    setActionModal(null);
    setCollectionAmount('');
    alert('Repayment of ' + formatINR(collectionAmount) + ' recorded successfully! Status updated to ACTIVE.');
  };

  return (
    <div className="container" style={{ padding: '40px 20px 80px' }}>
      {/* Top Banner with Role-Specific Greetings */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px', flexWrap: 'wrap', gap: '20px' }}>
        <div>
          <span style={{ fontSize: '13px', color: '#60a5fa', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            {isCustomer ? 'Borrower Self-Service Portal' : 'Operational Staff Console'}
          </span>
          <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: '32px', fontWeight: 800, color: '#fff', marginTop: '4px' }}>
            Welcome back, {user?.name || 'Customer'} 👋
          </h1>
          <p style={{ color: '#94a3b8', fontSize: '14px' }}>
            Indian Bank Digital Lending Suite — Active Session
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          {isCustomer ? (
            <Link to="/apply-loan" className="btn-primary" id="dash-apply-btn">
              + Apply New Loan
            </Link>
          ) : (
            <Link to="/loan-search" className="btn-secondary">
              🔍 Search Loans
            </Link>
          )}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* CUSTOMER DASHBOARD VIEW                                                  */}
      {/* ========================================================================= */}
      {isCustomer && (
        <>
          {/* Customer Top Metrics Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px', marginBottom: '32px' }}>
            <div className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Active Loans</div>
              <div style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#fff', marginTop: '6px' }}>
                {customerLoans.filter(l => l.status === 'ACTIVE' || l.status === 'OVERDUE').length}
              </div>
              <div style={{ fontSize: '12px', color: '#34d399', marginTop: '4px' }}>✓ In good standing</div>
            </div>

            <div className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Total Borrowed</div>
              <div style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#60a5fa', marginTop: '6px' }}>
                {formatINR(customerLoans.reduce((acc, l) => acc + l.amount, 0))}
              </div>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>Fixed rate @ 10.5% p.a.</div>
            </div>

            <div className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Total Balance Due</div>
              <div style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#f59e0b', marginTop: '6px' }}>
                {formatINR(customerLoans.reduce((acc, l) => acc + l.remainingBalance, 0))}
              </div>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>Over 21 remaining months</div>
            </div>

            <div className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Auto-Debit CBS Balance</div>
              <div style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#34d399', marginTop: '6px' }}>
                {formatINR(customerLoans[0]?.linkedAccount?.liveBalance || 145820)}
              </div>
              <div style={{ fontSize: '12px', color: '#34d399', marginTop: '4px' }}>🟢 Live CBS Connected</div>
            </div>
          </div>

          {/* Customer Loans Table */}
          <div className="glass-panel" style={{ padding: '30px', marginBottom: '32px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '20px', fontWeight: 700, color: '#fff' }}>
                My Loan Portfolio
              </h3>
              <span style={{ fontSize: '13px', color: '#94a3b8' }}>
                Showing {customerLoans.length} Loans
              </span>
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>
                    <th style={{ padding: '12px 16px' }}>Loan ID</th>
                    <th style={{ padding: '12px 16px' }}>Purpose</th>
                    <th style={{ padding: '12px 16px' }}>Amount</th>
                    <th style={{ padding: '12px 16px' }}>Monthly EMI</th>
                    <th style={{ padding: '12px 16px' }}>Progress</th>
                    <th style={{ padding: '12px 16px' }}>Status</th>
                    <th style={{ padding: '12px 16px', textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {customerLoans.map(loan => (
                    <tr key={loan.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#e2e8f0' }}>
                      <td style={{ padding: '16px' }}>
                        <Link to={`/loan-details/${loan.id}`} style={{ color: '#60a5fa', fontWeight: 700, textDecoration: 'none' }}>
                          {loan.loanNumber}
                        </Link>
                      </td>
                      <td style={{ padding: '16px' }}>{loan.purpose}</td>
                      <td style={{ padding: '16px', fontWeight: 700 }}>{formatINR(loan.amount)}</td>
                      <td style={{ padding: '16px', color: '#34d399', fontWeight: 700 }}>{formatINR(loan.monthlyEmi)}</td>
                      <td style={{ padding: '16px' }}>
                        <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '4px' }}>
                          {loan.installmentsPaid} of {loan.installmentsTotal} paid
                        </div>
                        <div style={{ width: '120px', height: '6px', background: '#1e293b', borderRadius: '3px', overflow: 'hidden' }}>
                          <div style={{ width: `${Math.round((loan.installmentsPaid / loan.installmentsTotal) * 100)}%`, height: '100%', background: '#3b82f6' }} />
                        </div>
                      </td>
                      <td style={{ padding: '16px' }}>
                        <span style={{
                          background: loan.status === 'ACTIVE' ? 'rgba(16,185,129,0.15)' : loan.status === 'OVERDUE' ? 'rgba(239,68,68,0.2)' : loan.status === 'APPROVED' ? 'rgba(59,130,246,0.2)' : 'rgba(245,158,11,0.2)',
                          color: loan.status === 'ACTIVE' ? '#34d399' : loan.status === 'OVERDUE' ? '#f87171' : loan.status === 'APPROVED' ? '#60a5fa' : '#f59e0b',
                          padding: '4px 10px',
                          borderRadius: '8px',
                          fontSize: '12px',
                          fontWeight: 700
                        }}>
                          {loan.status} {loan.dpdDays > 0 ? `(${loan.dpdDays} DPD)` : ''}
                        </span>
                      </td>
                      <td style={{ padding: '16px', textAlign: 'right' }}>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                          <Link to={`/loan-details/${loan.id}`} className="btn-secondary" style={{ padding: '6px 12px', fontSize: '12px' }}>
                            View Schedule
                          </Link>
                          {(loan.status === 'ACTIVE' || loan.status === 'OVERDUE') && (
                            <Link to={`/pay?loanId=${loan.id}`} className="btn-primary" style={{ padding: '6px 14px', fontSize: '12px' }}>
                              Pay EMI
                            </Link>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* ========================================================================= */}
      {/* STAFF & ADMIN WORKFLOW DASHBOARD VIEW                                     */}
      {/* ========================================================================= */}
      {!isCustomer && (
        <>
          {/* Staff Metrics */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px', marginBottom: '32px' }}>
            <div className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Pending Approvals</div>
              <div style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#f59e0b', marginTop: '6px' }}>
                {pendingLoans.length}
              </div>
              <div style={{ fontSize: '12px', color: '#f59e0b', marginTop: '4px' }}>Requires Loan Officer review</div>
            </div>

            <div className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Active Portfolio</div>
              <div style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#34d399', marginTop: '6px' }}>
                {formatINR(loans.filter(l => l.status === 'ACTIVE').reduce((acc, l) => acc + l.amount, 0))}
              </div>
              <div style={{ fontSize: '12px', color: '#34d399', marginTop: '4px' }}>Healthy recurring repayments</div>
            </div>

            <div className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Overdue DPD Queue</div>
              <div style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#f87171', marginTop: '6px' }}>
                {overdueLoans.length}
              </div>
              <div style={{ fontSize: '12px', color: '#f87171', marginTop: '4px' }}>Collections follow-up required</div>
            </div>

            <div className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Standard APR Setting</div>
              <div style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#60a5fa', marginTop: '6px' }}>
                10.5%
              </div>
              <div style={{ fontSize: '12px', color: '#60a5fa', marginTop: '4px' }}>Indian Bank Policy Locked</div>
            </div>
          </div>

          {/* Pending Origination Queue for Loan Officers */}
          {isOfficerOrAdmin && (
            <div className="glass-panel" style={{ padding: '30px', marginBottom: '32px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <div>
                  <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '20px', fontWeight: 700, color: '#fff' }}>
                    📋 Pending Loan Origination Queue
                  </h3>
                  <p style={{ fontSize: '13px', color: '#94a3b8', marginTop: '2px' }}>
                    Applications awaiting Loan Officer credit risk sign-off
                  </p>
                </div>
                <span style={{ background: 'rgba(245,158,11,0.2)', color: '#f59e0b', padding: '4px 12px', borderRadius: '12px', fontSize: '12px', fontWeight: 700 }}>
                  {pendingLoans.length} Pending Actions
                </span>
              </div>

              {pendingLoans.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
                  ✓ All loan applications have been processed.
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>
                        <th style={{ padding: '12px 16px' }}>Loan ID</th>
                        <th style={{ padding: '12px 16px' }}>Applicant</th>
                        <th style={{ padding: '12px 16px' }}>Requested Amount</th>
                        <th style={{ padding: '12px 16px' }}>Terms</th>
                        <th style={{ padding: '12px 16px' }}>Purpose</th>
                        <th style={{ padding: '12px 16px', textAlign: 'right' }}>Officer Decision</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingLoans.map(loan => (
                        <tr key={loan.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#e2e8f0' }}>
                          <td style={{ padding: '16px', fontWeight: 700 }}>{loan.loanNumber}</td>
                          <td style={{ padding: '16px' }}>
                            <div style={{ fontWeight: 600 }}>{loan.customerName}</div>
                            <div style={{ fontSize: '12px', color: '#64748b' }}>{loan.customerEmail}</div>
                          </td>
                          <td style={{ padding: '16px', fontWeight: 800, color: '#60a5fa' }}>{formatINR(loan.amount)}</td>
                          <td style={{ padding: '16px' }}>10.5% • 24 Mos</td>
                          <td style={{ padding: '16px' }}>{loan.purpose}</td>
                          <td style={{ padding: '16px', textAlign: 'right' }}>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                              <button 
                                type="button"
                                onClick={() => { setSelectedLoan(loan); setActionModal('approve'); }}
                                style={{ background: 'rgba(16,185,129,0.2)', border: '1px solid rgba(16,185,129,0.4)', color: '#34d399', padding: '6px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer' }}
                              >
                                ✓ Approve
                              </button>
                              <button 
                                type="button"
                                onClick={() => { setSelectedLoan(loan); setActionModal('reject'); }}
                                style={{ background: 'rgba(239,68,68,0.2)', border: '1px solid rgba(239,68,68,0.4)', color: '#f87171', padding: '6px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer' }}
                              >
                                ✗ Reject
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Overdue Collections Queue for Collections Staff */}
          {isCollectionsStaff && (
            <div className="glass-panel" style={{ padding: '30px', marginBottom: '32px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <div>
                  <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '20px', fontWeight: 700, color: '#fff' }}>
                    🚨 Collections & Delinquency Queue (DPD Reminders)
                  </h3>
                  <p style={{ fontSize: '13px', color: '#94a3b8', marginTop: '2px' }}>
                    Overdue accounts flagged by automated reminder scheduler
                  </p>
                </div>
                <Link to="/admin/reminders" className="btn-outline-glow" style={{ fontSize: '12px' }}>
                  Manage Reminders Console ➔
                </Link>
              </div>

              {overdueLoans.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
                  ✓ Zero delinquent loans in the queue.
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>
                        <th style={{ padding: '12px 16px' }}>Loan ID</th>
                        <th style={{ padding: '12px 16px' }}>Borrower</th>
                        <th style={{ padding: '12px 16px' }}>DPD Stage</th>
                        <th style={{ padding: '12px 16px' }}>EMI Overdue</th>
                        <th style={{ padding: '12px 16px' }}>Late Penalty</th>
                        <th style={{ padding: '12px 16px' }}>Total Due</th>
                        <th style={{ padding: '12px 16px', textAlign: 'right' }}>Staff Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {overdueLoans.map(loan => (
                        <tr key={loan.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#e2e8f0' }}>
                          <td style={{ padding: '16px', fontWeight: 700 }}>{loan.loanNumber}</td>
                          <td style={{ padding: '16px' }}>
                            <div style={{ fontWeight: 600 }}>{loan.customerName}</div>
                            <div style={{ fontSize: '12px', color: '#94a3b8' }}>{loan.customerPhone}</div>
                          </td>
                          <td style={{ padding: '16px' }}>
                            <span style={{ background: 'rgba(239,68,68,0.2)', color: '#f87171', padding: '4px 8px', borderRadius: '6px', fontSize: '11px', fontWeight: 800 }}>
                              STAGE: DPD {loan.dpdDays}
                            </span>
                          </td>
                          <td style={{ padding: '16px' }}>{formatINR(loan.monthlyEmi)}</td>
                          <td style={{ padding: '16px', color: '#f59e0b', fontWeight: 700 }}>{formatINR(loan.penalty)}</td>
                          <td style={{ padding: '16px', color: '#f87171', fontWeight: 800 }}>{formatINR(loan.monthlyEmi + loan.penalty)}</td>
                          <td style={{ padding: '16px', textAlign: 'right' }}>
                            <button 
                              type="button"
                              onClick={() => { setSelectedLoan(loan); setActionModal('recordCollection'); setCollectionAmount(loan.monthlyEmi + loan.penalty); }}
                              className="btn-primary" 
                              style={{ padding: '6px 14px', fontSize: '12px' }}
                            >
                              Record Payment
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* ========================================================================= */}
      {/* ACTION MODAL DIALOGS                                                      */}
      {/* ========================================================================= */}
      {actionModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(8px)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '480px', padding: '32px' }}>
            <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '14px' }}>
              {actionModal === 'approve' && `Approve Loan ${selectedLoan?.loanNumber}`}
              {actionModal === 'reject' && `Reject Loan ${selectedLoan?.loanNumber}`}
              {actionModal === 'recordCollection' && `Record Collection for ${selectedLoan?.customerName}`}
            </h3>

            {actionModal === 'approve' && (
              <div>
                <p style={{ fontSize: '14px', color: '#94a3b8', marginBottom: '20px' }}>
                  Are you sure you want to approve this loan of <strong>{formatINR(selectedLoan?.amount)}</strong> at the standard Indian Bank rate of <strong>10.5% p.a.</strong>?
                </p>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                  <button type="button" onClick={() => setActionModal(null)} className="btn-secondary" style={{ padding: '8px 16px' }}>Cancel</button>
                  <button type="button" onClick={() => handleApprove(selectedLoan?.id)} className="btn-primary" style={{ padding: '8px 20px', background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', color: '#fff' }}>Confirm Approval</button>
                </div>
              </div>
            )}

            {actionModal === 'reject' && (
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
                  Reason for Rejection *
                </label>
                <textarea 
                  value={actionReason}
                  onChange={(e) => setActionReason(e.target.value)}
                  placeholder="e.g. Credit score below 650, insufficient verifiable income"
                  required
                  rows="3"
                  style={{ width: '100%', padding: '12px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '10px', color: '#fff', fontSize: '14px', outline: 'none', marginBottom: '20px' }}
                />
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                  <button type="button" onClick={() => setActionModal(null)} className="btn-secondary" style={{ padding: '8px 16px' }}>Cancel</button>
                  <button type="button" onClick={() => handleReject(selectedLoan?.id)} className="btn-primary" style={{ padding: '8px 20px', background: 'linear-gradient(135deg, #ef4444 0%, #b91c1c 100%)', color: '#fff' }}>Confirm Rejection</button>
                </div>
              </div>
            )}

            {actionModal === 'recordCollection' && (
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
                  Collected Amount (₹) *
                </label>
                <input 
                  type="number"
                  value={collectionAmount}
                  onChange={(e) => setCollectionAmount(e.target.value)}
                  placeholder="Amount collected"
                  required
                  style={{ width: '100%', padding: '12px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '10px', color: '#fff', fontSize: '16px', fontWeight: 700, outline: 'none', marginBottom: '20px' }}
                />
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                  <button type="button" onClick={() => setActionModal(null)} className="btn-secondary" style={{ padding: '8px 16px' }}>Cancel</button>
                  <button type="button" onClick={() => handleRecordCollection(selectedLoan?.id)} className="btn-primary" style={{ padding: '8px 20px' }}>Submit Collection</button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
