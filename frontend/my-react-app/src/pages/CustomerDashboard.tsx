import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { loanApi } from '../api/loanApi';
import { useAuth } from '../context/AuthContext';
import { StatCard } from '../components/StatCard';
import {
  CreditCard,
  Wallet,
  Clock,
  CheckCircle2,
  AlertCircle,
  FileText,
  ArrowUpRight,
  PlusCircle,
  Calendar,
  X
} from 'lucide-react';
import { Loan, CustomerActivityDto, RepaymentInstallment } from '../types';

export const CustomerDashboard: React.FC = () => {
  const { authState } = useAuth();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  // Modal for viewing loan schedule
  const [selectedLoan, setSelectedLoan] = useState<Loan | null>(null);
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [scheduleData, setScheduleData] = useState<RepaymentInstallment[]>([]);

  useEffect(() => {
    let isMounted = true;
    async function fetchDashboard() {
      try {
        const res = await loanApi.getDashboardData();
        if (isMounted) {
          setData(res);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.response?.data?.error || err.message || 'Failed to load dashboard.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    fetchDashboard();
    return () => { isMounted = false; };
  }, []);

  const handleOpenSchedule = async (loan: Loan) => {
    setSelectedLoan(loan);
    setScheduleLoading(true);
    try {
      const details = await loanApi.getLoanAccountDetails(loan.id);
      setScheduleData(details.installments || []);
    } catch {
      setScheduleData([]);
    } finally {
      setScheduleLoading(false);
    }
  };

  const formatCurrency = (val: number | undefined) => {
    if (val === undefined || val === null) return '₹0.00';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2
    }).format(val);
  };

  const getStatusBadge = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'ACTIVE':
        return <span style={{ backgroundColor: '#DCFCE7', color: '#166534', padding: '3px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 700 }}>ACTIVE</span>;
      case 'PENDING':
      case 'SUBMITTED':
      case 'IN_REVIEW':
        return <span style={{ backgroundColor: '#FEF9C3', color: '#854D0E', padding: '3px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 700 }}>PENDING REVIEW</span>;
      case 'CLOSED':
        return <span style={{ backgroundColor: '#E0F2FE', color: '#075985', padding: '3px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 700 }}>CLOSED</span>;
      case 'REJECTED':
        return <span style={{ backgroundColor: '#FEE2E2', color: '#991B1B', padding: '3px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 700 }}>REJECTED</span>;
      default:
        return <span style={{ backgroundColor: '#F1F5F9', color: '#475569', padding: '3px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 700 }}>{status}</span>;
    }
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '1280px', margin: '40px auto', padding: '0 20px', textAlign: 'center' }}>
        <div style={{
          width: '42px',
          height: '42px',
          border: '4px solid #E2E8F0',
          borderTopColor: '#0050A4',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite',
          margin: '0 auto 16px'
        }} />
        <p style={{ color: '#0A2E65', fontWeight: 600 }}>Loading NetBanking Loan Portfolio...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ maxWidth: '1280px', margin: '40px auto', padding: '0 20px' }}>
        <div style={{
          backgroundColor: '#FEF2F2',
          border: '1px solid #FEE2E2',
          color: '#991B1B',
          padding: '20px',
          borderRadius: '8px',
          textAlign: 'center'
        }}>
          <AlertCircle size={32} style={{ margin: '0 auto 8px', color: '#DC2626' }} />
          <h3>Error Loading Dashboard</h3>
          <p>{error}</p>
        </div>
      </div>
    );
  }

  const loans: Loan[] = data?.loans || [];
  const activities: CustomerActivityDto[] = data?.recentActivities || [];

  return (
    <div style={{ maxWidth: '1280px', margin: '32px auto', padding: '0 20px' }}>
      {/* Welcome Banner */}
      <div style={{
        backgroundColor: '#0050A4',
        backgroundImage: 'linear-gradient(135deg, #0050A4 0%, #0A2E65 100%)',
        borderRadius: '12px',
        padding: '28px 32px',
        color: '#FFFFFF',
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '20px',
        marginBottom: '28px',
        boxShadow: '0 8px 20px rgba(0, 80, 164, 0.2)'
      }}>
        <div>
          <span style={{
            fontSize: '11px',
            fontWeight: 700,
            letterSpacing: '1px',
            backgroundColor: 'rgba(255, 255, 255, 0.15)',
            padding: '4px 10px',
            borderRadius: '12px',
            textTransform: 'uppercase'
          }}>
            Net Banking Customer Portal
          </span>
          <h1 style={{ fontSize: '26px', fontWeight: 800, margin: '10px 0 6px' }}>
            Welcome back, {authState.user?.name || 'Customer'}
          </h1>
          <p style={{ fontSize: '13px', color: '#BFDBFE', margin: 0 }}>
            Customer ID: <strong>{authState.user?.customerId || `CUST-${authState.user?.id}`}</strong> • Home Branch: <strong>{authState.user?.branch || 'MAIN'}</strong>
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <Link
            to="/applyLoan"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 22px',
              backgroundColor: '#ED7D16',
              color: '#FFFFFF',
              borderRadius: '6px',
              textDecoration: 'none',
              fontWeight: 700,
              fontSize: '14px',
              boxShadow: '0 4px 12px rgba(237, 125, 22, 0.4)'
            }}
          >
            <PlusCircle size={18} />
            Apply New Loan
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
        gap: '20px',
        marginBottom: '32px'
      }}>
        <StatCard
          title="Outstanding Balance"
          value={formatCurrency(data?.totalRemaining || 0)}
          subtitle="Remaining principal across active accounts"
          icon={<Wallet size={20} />}
          color="#0050A4"
        />
        <StatCard
          title="Monthly EMI Due"
          value={formatCurrency(data?.totalEmi || 0)}
          subtitle="Total regular monthly commitment"
          icon={<CreditCard size={20} />}
          color="#ED7D16"
        />
        <StatCard
          title="Active Loan Accounts"
          value={data?.activeLoanCount || 0}
          subtitle={`Total loan applications: ${data?.totalLoanCount || 0}`}
          icon={<CheckCircle2 size={20} />}
          color="#16A34A"
        />
        <StatCard
          title="Pending Review"
          value={data?.pendingLoanCount || 0}
          subtitle="Applications awaiting credit sanction"
          icon={<Clock size={20} />}
          color="#E31B23"
        />
      </div>

      {/* Main Content: Loans Table + Activity Sidebar */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '2.5fr 1fr',
        gap: '28px',
        alignItems: 'flex-start'
      }} className="customer-dashboard-grid">
        {/* Left Column: Loan Accounts Table */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden'
        }}>
          <div style={{
            padding: '20px 24px',
            borderBottom: '1px solid #E2E8F0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <div>
              <h2 style={{ fontSize: '18px', fontWeight: 800, color: '#0A2E65', margin: 0 }}>
                My Loan Accounts
              </h2>
              <p style={{ fontSize: '12px', color: '#64748B', margin: '4px 0 0' }}>
                View balance, repayment schedule, and make instant online payments
              </p>
            </div>
            <Link
              to="/applyLoan"
              style={{
                fontSize: '13px',
                fontWeight: 600,
                color: '#0050A4',
                textDecoration: 'none',
                display: 'flex',
                alignItems: 'center',
                gap: '4px'
              }}
            >
              Apply for More <ArrowUpRight size={14} />
            </Link>
          </div>

          {loans.length === 0 ? (
            <div style={{ padding: '48px 20px', textAlign: 'center' }}>
              <CreditCard size={40} style={{ color: '#94A3B8', margin: '0 auto 12px' }} />
              <h3 style={{ fontSize: '16px', color: '#0A2E65', marginBottom: '6px' }}>No Loan Accounts Found</h3>
              <p style={{ fontSize: '13px', color: '#64748B', marginBottom: '20px' }}>
                You have not applied for any loans yet. Calculate your EMI and apply today!
              </p>
              <Link to="/applyLoan" className="btn" style={{ textDecoration: 'none' }}>
                Apply for First Loan
              </Link>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ backgroundColor: '#F8FAFC', color: '#475569', borderBottom: '1px solid #E2E8F0' }}>
                    <th style={{ padding: '12px 16px', fontWeight: 700 }}>Loan ID</th>
                    <th style={{ padding: '12px 16px', fontWeight: 700 }}>Sanction Amount</th>
                    <th style={{ padding: '12px 16px', fontWeight: 700 }}>Interest</th>
                    <th style={{ padding: '12px 16px', fontWeight: 700 }}>Monthly EMI</th>
                    <th style={{ padding: '12px 16px', fontWeight: 700 }}>Remaining Balance</th>
                    <th style={{ padding: '12px 16px', fontWeight: 700 }}>Status</th>
                    <th style={{ padding: '12px 16px', fontWeight: 700, textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loans.map((loan) => (
                    <tr
                      key={loan.id}
                      style={{
                        borderBottom: '1px solid #F1F5F9',
                        transition: 'background-color 0.15s ease'
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#F8FAFC')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                    >
                      <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0050A4' }}>
                        #{loan.id}
                      </td>
                      <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0A2E65' }}>
                        {formatCurrency(loan.amount)}
                      </td>
                      <td style={{ padding: '14px 16px', color: '#475569' }}>
                        {loan.interest}% p.a.
                      </td>
                      <td style={{ padding: '14px 16px', fontWeight: 600, color: '#0A2E65' }}>
                        {formatCurrency(loan.emi)}
                      </td>
                      <td style={{ padding: '14px 16px', fontWeight: 700, color: loan.remaining > 0 ? '#C2410C' : '#16A34A' }}>
                        {formatCurrency(loan.remaining)}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        {getStatusBadge(loan.status)}
                      </td>
                      <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '8px' }}>
                          <button
                            type="button"
                            onClick={() => handleOpenSchedule(loan)}
                            style={{
                              padding: '6px 10px',
                              backgroundColor: '#EFF6FF',
                              border: '1px solid #BFDBFE',
                              color: '#0050A4',
                              borderRadius: '4px',
                              fontSize: '12px',
                              fontWeight: 600,
                              cursor: 'pointer'
                            }}
                          >
                            Schedule
                          </button>

                          {loan.status === 'ACTIVE' && loan.remaining > 0 && (
                            <Link
                              to={`/pay?loanId=${loan.id}`}
                              style={{
                                padding: '6px 12px',
                                backgroundColor: '#16A34A',
                                color: '#FFFFFF',
                                borderRadius: '4px',
                                fontSize: '12px',
                                fontWeight: 700,
                                textDecoration: 'none'
                              }}
                            >
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
          )}
        </div>

        {/* Right Column: Recent Activities Sidebar */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          padding: '20px 22px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#0A2E65', marginBottom: '4px' }}>
            Recent Activity
          </h3>
          <p style={{ fontSize: '12px', color: '#64748B', marginBottom: '16px' }}>
            Latest account updates & transaction receipts
          </p>

          {activities.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '24px 0', color: '#94A3B8', fontSize: '13px' }}>
              No recent activity records.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {activities.map((act, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: '12px',
                    borderRadius: '8px',
                    backgroundColor: '#F8FAFC',
                    border: '1px solid #E2E8F0',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '4px'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span style={{ fontSize: '13px', fontWeight: 700, color: '#0A2E65' }}>
                      {act.title}
                    </span>
                    <span style={{ fontSize: '11px', color: '#94A3B8' }}>
                      {act.timestamp}
                    </span>
                  </div>
                  <p style={{ fontSize: '12px', color: '#475569', margin: 0 }}>
                    {act.description}
                  </p>
                </div>
              ))}
            </div>
          )}

          {/* Quick Support Card */}
          <div style={{
            marginTop: '20px',
            padding: '14px',
            backgroundColor: '#EFF6FF',
            borderRadius: '8px',
            border: '1px solid #DBEAFE',
            fontSize: '12px',
            color: '#1E40AF'
          }}>
            <strong>Need loan assistance?</strong>
            <p style={{ margin: '4px 0 0', color: '#3B82F6' }}>
              Call our dedicated loan desk at 1800-220-229 or visit your nearest branch.
            </p>
          </div>
        </div>
      </div>

      {/* Repayment Schedule Modal */}
      {selectedLoan && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(10, 46, 101, 0.6)',
          backdropFilter: 'blur(3px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
          padding: '20px'
        }}>
          <div style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '12px',
            maxWidth: '750px',
            width: '100%',
            maxHeight: '85vh',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.25)'
          }}>
            <div style={{
              padding: '20px 24px',
              borderBottom: '1px solid #E2E8F0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#0A2E65', margin: 0 }}>
                  Loan Repayment Schedule: #{selectedLoan.id}
                </h3>
                <span style={{ fontSize: '12px', color: '#64748B' }}>
                  Principal: {formatCurrency(selectedLoan.amount)} • Monthly EMI: {formatCurrency(selectedLoan.emi)}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setSelectedLoan(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748B' }}
              >
                <X size={20} />
              </button>
            </div>

            <div style={{ padding: '20px 24px', overflowY: 'auto', flex: 1 }}>
              {scheduleLoading ? (
                <div style={{ textAlign: 'center', padding: '40px 0' }}>
                  <p>Loading amortization schedule...</p>
                </div>
              ) : scheduleData.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '30px 0', color: '#64748B' }}>
                  Schedule not yet generated or pending sanction.
                </div>
              ) : (
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                  <thead>
                    <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0' }}>
                      <th style={{ padding: '10px 12px' }}>#</th>
                      <th style={{ padding: '10px 12px' }}>Due Date</th>
                      <th style={{ padding: '10px 12px' }}>Principal</th>
                      <th style={{ padding: '10px 12px' }}>Interest</th>
                      <th style={{ padding: '10px 12px' }}>Total Due</th>
                      <th style={{ padding: '10px 12px' }}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scheduleData.map((inst) => (
                      <tr key={inst.installmentNumber} style={{ borderBottom: '1px solid #F1F5F9' }}>
                        <td style={{ padding: '10px 12px', fontWeight: 600 }}>{inst.installmentNumber}</td>
                        <td style={{ padding: '10px 12px' }}>{inst.dueDate}</td>
                        <td style={{ padding: '10px 12px' }}>{formatCurrency(inst.principalDue)}</td>
                        <td style={{ padding: '10px 12px' }}>{formatCurrency(inst.interestDue)}</td>
                        <td style={{ padding: '10px 12px', fontWeight: 700 }}>{formatCurrency(inst.totalDue)}</td>
                        <td style={{ padding: '10px 12px' }}>
                          <span style={{
                            fontSize: '11px',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '10px',
                            backgroundColor: inst.status === 'PAID' ? '#DCFCE7' : (inst.dpd > 0 ? '#FEE2E2' : '#FEF9C3'),
                            color: inst.status === 'PAID' ? '#166534' : (inst.dpd > 0 ? '#991B1B' : '#854D0E')
                          }}>
                            {inst.status} {inst.dpd > 0 ? `(${inst.dpd} DPD)` : ''}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div style={{
              padding: '14px 24px',
              borderTop: '1px solid #E2E8F0',
              backgroundColor: '#F8FAFC',
              display: 'flex',
              justifyContent: 'flex-end'
            }}>
              <button
                type="button"
                onClick={() => setSelectedLoan(null)}
                style={{
                  padding: '8px 18px',
                  backgroundColor: '#0050A4',
                  color: '#FFFFFF',
                  borderRadius: '6px',
                  border: 'none',
                  fontWeight: 600,
                  fontSize: '13px',
                  cursor: 'pointer'
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      <style>{`
        @media (max-width: 900px) {
          .customer-dashboard-grid {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  );
};

export default CustomerDashboard;
