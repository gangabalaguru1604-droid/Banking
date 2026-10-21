import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { loanApi } from '../api/loanApi';
import { useAuth } from '../context/AuthContext';
import { StatCard } from '../components/StatCard';
import {
  Briefcase,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Search,
  Filter,
  DollarSign,
  TrendingDown,
  UserCheck,
  ChevronRight,
  X,
  CreditCard
} from 'lucide-react';
import { StaffDashboardLoanDto, PaymentTransaction } from '../types';

export const StaffDashboard: React.FC = () => {
  const { authState } = useAuth();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'pending' | 'delinquent' | 'active' | 'collections'>('pending');

  // Modals
  const [reviewLoan, setReviewLoan] = useState<StaffDashboardLoanDto | null>(null);
  const [rejectionReason, setRejectionReason] = useState<string>('');
  const [actionLoading, setActionLoading] = useState<boolean>(false);

  // Collections recording modal
  const [collectLoan, setCollectLoan] = useState<StaffDashboardLoanDto | null>(null);
  const [collectAmount, setCollectAmount] = useState<number>(0);
  const [collectMode, setCollectMode] = useState<string>('UPI');
  const [collectRef, setCollectRef] = useState<string>('');
  const [collectRemarks, setCollectRemarks] = useState<string>('');

  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);

  const fetchDashboard = async () => {
    try {
      const res = await loanApi.getDashboardData();
      setData(res);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load staff portfolio.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  const formatCurrency = (val: number | undefined) => {
    if (val === undefined || val === null) return '₹0.00';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val);
  };

  const handleApprove = async () => {
    if (!reviewLoan) return;
    setActionLoading(true);
    try {
      const res = await loanApi.approveLoan(reviewLoan.id);
      if (res.success) {
        setFeedbackMessage(`Loan #${reviewLoan.id} approved and disbursed successfully.`);
        setReviewLoan(null);
        await fetchDashboard();
      } else {
        alert(res.error || 'Approval failed.');
      }
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Approval failed.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!reviewLoan) return;
    setActionLoading(true);
    try {
      const res = await loanApi.rejectLoan(reviewLoan.id, rejectionReason);
      if (res.success) {
        setFeedbackMessage(`Loan #${reviewLoan.id} marked as rejected.`);
        setReviewLoan(null);
        setRejectionReason('');
        await fetchDashboard();
      } else {
        alert(res.error || 'Rejection failed.');
      }
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Rejection failed.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRecordCollection = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!collectLoan) return;
    setActionLoading(true);
    try {
      const res = await loanApi.recordCollection({
        loanId: collectLoan.id,
        amount: collectAmount,
        paymentMode: collectMode,
        referenceNumber: collectRef || `COL-${Date.now().toString().slice(-6)}`,
        collectionRemarks: collectRemarks
      });
      if (res.success) {
        setFeedbackMessage(`Collection of ${formatCurrency(collectAmount)} recorded for Loan #${collectLoan.id}.`);
        setCollectLoan(null);
        await fetchDashboard();
      } else {
        alert(res.error || 'Failed to record collection.');
      }
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Failed to record collection.');
    } finally {
      setActionLoading(false);
    }
  };

  const getDpdBadge = (dpd: number) => {
    if (dpd === 0) {
      return <span style={{ backgroundColor: '#DCFCE7', color: '#166534', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>Current (0)</span>;
    } else if (dpd <= 30) {
      return <span style={{ backgroundColor: '#FEF9C3', color: '#854D0E', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>SMA-0 ({dpd}d)</span>;
    } else if (dpd <= 60) {
      return <span style={{ backgroundColor: '#FFEDD5', color: '#C2410C', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>SMA-1 ({dpd}d)</span>;
    } else if (dpd <= 90) {
      return <span style={{ backgroundColor: '#FEE2E2', color: '#991B1B', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>SMA-2 ({dpd}d)</span>;
    } else {
      return <span style={{ backgroundColor: '#7F1D1D', color: '#FFFFFF', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>NPA ({dpd}d)</span>;
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
        <p style={{ color: '#0A2E65', fontWeight: 600 }}>Loading Credit & Operations Portfolio...</p>
      </div>
    );
  }

  const pendingLoans: StaffDashboardLoanDto[] = data?.pendingLoans || [];
  const delinquentLoans: StaffDashboardLoanDto[] = data?.delinquentLoans || [];
  const activeLoans: StaffDashboardLoanDto[] = data?.activeLoans || [];
  const recentCollections: PaymentTransaction[] = data?.recentCollections || [];

  return (
    <div style={{ maxWidth: '1280px', margin: '32px auto', padding: '0 20px' }}>
      {/* Staff Header Banner */}
      <div style={{
        backgroundColor: '#0A2E65',
        borderRadius: '12px',
        padding: '24px 32px',
        color: '#FFFFFF',
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '20px',
        marginBottom: '28px'
      }}>
        <div>
          <span style={{
            fontSize: '11px',
            fontWeight: 700,
            letterSpacing: '1px',
            backgroundColor: '#0050A4',
            padding: '4px 10px',
            borderRadius: '12px',
            textTransform: 'uppercase'
          }}>
            Institutional Staff Dashboard
          </span>
          <h1 style={{ fontSize: '24px', fontWeight: 800, margin: '8px 0 4px' }}>
            Loan Operations & Delinquency Management
          </h1>
          <p style={{ fontSize: '13px', color: '#94A3B8', margin: 0 }}>
            Officer: <strong>{authState.user?.name}</strong> • Role: <strong>{data?.userRole}</strong> • Branch Jurisdiction: <strong>{data?.officerBranch}</strong>
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <Link
            to="/loan-search"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '10px 18px',
              backgroundColor: '#0050A4',
              color: '#FFFFFF',
              borderRadius: '6px',
              textDecoration: 'none',
              fontWeight: 600,
              fontSize: '13px'
            }}
          >
            <Search size={16} /> Advanced Loan Search
          </Link>
        </div>
      </div>

      {feedbackMessage && (
        <div style={{
          backgroundColor: '#F0FDF4',
          border: '1px solid #BBF7D0',
          color: '#166534',
          padding: '12px 16px',
          borderRadius: '8px',
          fontSize: '13px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckCircle2 size={18} />
            <span>{feedbackMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setFeedbackMessage(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#166534' }}
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* Top 4 KPI Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
        gap: '20px',
        marginBottom: '28px'
      }}>
        <StatCard
          title="Sanctioned Portfolio"
          value={formatCurrency(data?.totalPrincipal || 0)}
          subtitle={`Total loan accounts: ${data?.totalLoansCount || 0}`}
          icon={<DollarSign size={20} />}
          color="#0050A4"
        />
        <StatCard
          title="Outstanding Balance"
          value={formatCurrency(data?.totalRemaining || 0)}
          subtitle={`Active accounts: ${data?.activeLoansCount || 0}`}
          icon={<Briefcase size={20} />}
          color="#3B82F6"
        />
        <StatCard
          title="Total Overdue Amount"
          value={formatCurrency(data?.totalOverdueAmount || 0)}
          subtitle={`Delinquent loans: ${data?.overdueLoansCount || 0}`}
          icon={<AlertTriangle size={20} />}
          color="#DC2626"
        />
        <StatCard
          title="Portfolio At Risk (PAR)"
          value={`${data?.parRate || 0}%`}
          subtitle="PAR > 0 DPD of total balance"
          icon={<TrendingDown size={20} />}
          color="#ED7D16"
        />
      </div>

      {/* RBI DPD Bucketing Matrix */}
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '10px',
        border: '1px solid #E2E8F0',
        padding: '20px',
        marginBottom: '28px',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <div>
            <h3 style={{ fontSize: '15px', fontWeight: 800, color: '#0A2E65', margin: 0 }}>
              RBI Delinquency Classification (IRAC Guidelines)
            </h3>
            <span style={{ fontSize: '12px', color: '#64748B' }}>
              Real-time monitoring of portfolio health across Days Past Due stages
            </span>
          </div>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '12px'
        }}>
          {/* Current */}
          <div style={{ padding: '12px', borderRadius: '8px', backgroundColor: '#F0FDF4', border: '1px solid #BBF7D0' }}>
            <span style={{ fontSize: '11px', fontWeight: 700, color: '#166534', textTransform: 'uppercase' }}>Current (0 DPD)</span>
            <div style={{ fontSize: '18px', fontWeight: 800, color: '#166534', margin: '4px 0' }}>{data?.dpdCurrentCount || 0} Accounts</div>
            <span style={{ fontSize: '12px', color: '#15803D' }}>{formatCurrency(data?.dpdCurrentAmount)}</span>
          </div>

          {/* SMA-0 */}
          <div style={{ padding: '12px', borderRadius: '8px', backgroundColor: '#FEF9C3', border: '1px solid #FEF08A' }}>
            <span style={{ fontSize: '11px', fontWeight: 700, color: '#854D0E', textTransform: 'uppercase' }}>SMA-0 (1-30 DPD)</span>
            <div style={{ fontSize: '18px', fontWeight: 800, color: '#854D0E', margin: '4px 0' }}>{data?.dpdSma0Count || 0} Accounts</div>
            <span style={{ fontSize: '12px', color: '#A16207' }}>{formatCurrency(data?.dpdSma0Amount)}</span>
          </div>

          {/* SMA-1 */}
          <div style={{ padding: '12px', borderRadius: '8px', backgroundColor: '#FFEDD5', border: '1px solid #FED7AA' }}>
            <span style={{ fontSize: '11px', fontWeight: 700, color: '#C2410C', textTransform: 'uppercase' }}>SMA-1 (31-60 DPD)</span>
            <div style={{ fontSize: '18px', fontWeight: 800, color: '#C2410C', margin: '4px 0' }}>{data?.dpdSma1Count || 0} Accounts</div>
            <span style={{ fontSize: '12px', color: '#C2410C' }}>{formatCurrency(data?.dpdSma1Amount)}</span>
          </div>

          {/* SMA-2 */}
          <div style={{ padding: '12px', borderRadius: '8px', backgroundColor: '#FEE2E2', border: '1px solid #FECACA' }}>
            <span style={{ fontSize: '11px', fontWeight: 700, color: '#991B1B', textTransform: 'uppercase' }}>SMA-2 (61-90 DPD)</span>
            <div style={{ fontSize: '18px', fontWeight: 800, color: '#991B1B', margin: '4px 0' }}>{data?.dpdSma2Count || 0} Accounts</div>
            <span style={{ fontSize: '12px', color: '#B91C1C' }}>{formatCurrency(data?.dpdSma2Amount)}</span>
          </div>

          {/* NPA */}
          <div style={{ padding: '12px', borderRadius: '8px', backgroundColor: '#450A0A', color: '#FFFFFF', border: '1px solid #7F1D1D' }}>
            <span style={{ fontSize: '11px', fontWeight: 700, color: '#FCA5A5', textTransform: 'uppercase' }}>NPA (&gt;90 DPD)</span>
            <div style={{ fontSize: '18px', fontWeight: 800, color: '#FFFFFF', margin: '4px 0' }}>{data?.dpdNpaCount || 0} Accounts</div>
            <span style={{ fontSize: '12px', color: '#F87171' }}>{formatCurrency(data?.dpdNpaAmount)}</span>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div style={{
        display: 'flex',
        gap: '8px',
        borderBottom: '2px solid #E2E8F0',
        marginBottom: '20px'
      }}>
        <button
          type="button"
          onClick={() => setActiveTab('pending')}
          style={{
            padding: '10px 18px',
            border: 'none',
            borderBottom: activeTab === 'pending' ? '3px solid #0050A4' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: activeTab === 'pending' ? '#0050A4' : '#64748B',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}
        >
          Pending Review
          <span style={{
            fontSize: '11px',
            backgroundColor: activeTab === 'pending' ? '#0050A4' : '#E2E8F0',
            color: activeTab === 'pending' ? '#FFFFFF' : '#475569',
            padding: '2px 8px',
            borderRadius: '10px'
          }}>
            {pendingLoans.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('delinquent')}
          style={{
            padding: '10px 18px',
            border: 'none',
            borderBottom: activeTab === 'delinquent' ? '3px solid #DC2626' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: activeTab === 'delinquent' ? '#DC2626' : '#64748B',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}
        >
          Delinquent & Collections
          <span style={{
            fontSize: '11px',
            backgroundColor: activeTab === 'delinquent' ? '#DC2626' : '#E2E8F0',
            color: activeTab === 'delinquent' ? '#FFFFFF' : '#475569',
            padding: '2px 8px',
            borderRadius: '10px'
          }}>
            {delinquentLoans.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('active')}
          style={{
            padding: '10px 18px',
            border: 'none',
            borderBottom: activeTab === 'active' ? '3px solid #16A34A' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: activeTab === 'active' ? '#16A34A' : '#64748B',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          Active Accounts ({activeLoans.length})
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('collections')}
          style={{
            padding: '10px 18px',
            border: 'none',
            borderBottom: activeTab === 'collections' ? '3px solid #ED7D16' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: activeTab === 'collections' ? '#ED7D16' : '#64748B',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          Recent Settlements ({recentCollections.length})
        </button>
      </div>

      {/* Tab 1: Pending Approvals */}
      {activeTab === 'pending' && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden'
        }}>
          {pendingLoans.length === 0 ? (
            <div style={{ padding: '48px 20px', textAlign: 'center', color: '#64748B' }}>
              <CheckCircle2 size={36} color="#16A34A" style={{ margin: '0 auto 8px' }} />
              <p>No pending loan applications requiring credit sanction in your branch jurisdiction.</p>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                    <th style={{ padding: '12px 16px' }}>Loan ID</th>
                    <th style={{ padding: '12px 16px' }}>Customer</th>
                    <th style={{ padding: '12px 16px' }}>Sanction Amount</th>
                    <th style={{ padding: '12px 16px' }}>Tenure</th>
                    <th style={{ padding: '12px 16px' }}>Branch</th>
                    <th style={{ padding: '12px 16px' }}>Applied Date</th>
                    <th style={{ padding: '12px 16px', textAlign: 'right' }}>Credit Decision</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingLoans.map((loan) => (
                    <tr key={loan.id} style={{ borderBottom: '1px solid #F1F5F9' }}>
                      <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0050A4' }}>
                        #{loan.id}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <Link to={`/customer-profile?customerId=${loan.customerId}`} style={{ fontWeight: 700, color: '#0A2E65', textDecoration: 'none' }}>
                          {loan.customerName}
                        </Link>
                        <div style={{ fontSize: '11px', color: '#64748B' }}>{loan.customerId}</div>
                      </td>
                      <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0A2E65' }}>
                        {formatCurrency(loan.amount)}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        {loan.tenureMonths} Months ({loan.interestRate}% p.a.)
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        {loan.branch}
                      </td>
                      <td style={{ padding: '14px 16px', color: '#64748B' }}>
                        {loan.appliedDate}
                      </td>
                      <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                        <button
                          type="button"
                          onClick={() => setReviewLoan(loan)}
                          style={{
                            padding: '6px 14px',
                            backgroundColor: '#0050A4',
                            color: '#FFFFFF',
                            border: 'none',
                            borderRadius: '4px',
                            fontWeight: 600,
                            fontSize: '12px',
                            cursor: 'pointer'
                          }}
                        >
                          Review & Sanction
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

      {/* Tab 2: Delinquent Loans */}
      {activeTab === 'delinquent' && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden'
        }}>
          {delinquentLoans.length === 0 ? (
            <div style={{ padding: '48px 20px', textAlign: 'center', color: '#64748B' }}>
              <CheckCircle2 size={36} color="#16A34A" style={{ margin: '0 auto 8px' }} />
              <p>Zero overdue or delinquent loan accounts in your assigned portfolio!</p>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                    <th style={{ padding: '12px 16px' }}>Loan ID</th>
                    <th style={{ padding: '12px 16px' }}>Borrower</th>
                    <th style={{ padding: '12px 16px' }}>DPD Stage</th>
                    <th style={{ padding: '12px 16px' }}>Overdue Amount</th>
                    <th style={{ padding: '12px 16px' }}>Total Outstanding</th>
                    <th style={{ padding: '12px 16px' }}>Branch</th>
                    <th style={{ padding: '12px 16px', textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {delinquentLoans.map((loan) => (
                    <tr key={loan.id} style={{ borderBottom: '1px solid #F1F5F9' }}>
                      <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0050A4' }}>
                        <Link to={`/loan-details?loanId=${loan.id}`} style={{ textDecoration: 'none', color: '#0050A4' }}>
                          #{loan.id}
                        </Link>
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <Link to={`/customer-profile?customerId=${loan.customerId}`} style={{ fontWeight: 700, color: '#0A2E65', textDecoration: 'none' }}>
                          {loan.customerName}
                        </Link>
                        <div style={{ fontSize: '11px', color: '#64748B' }}>{loan.customerEmail}</div>
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        {getDpdBadge(loan.dpd)}
                      </td>
                      <td style={{ padding: '14px 16px', fontWeight: 800, color: '#DC2626' }}>
                        {formatCurrency(loan.overdueAmount)}
                      </td>
                      <td style={{ padding: '14px 16px', fontWeight: 700 }}>
                        {formatCurrency(loan.remaining)}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        {loan.branch}
                      </td>
                      <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '8px' }}>
                          <button
                            type="button"
                            onClick={() => {
                              setCollectLoan(loan);
                              setCollectAmount(loan.overdueAmount || loan.emi);
                              setCollectRef(`COL-${Date.now().toString().slice(-6)}`);
                            }}
                            style={{
                              padding: '6px 12px',
                              backgroundColor: '#16A34A',
                              color: '#FFFFFF',
                              border: 'none',
                              borderRadius: '4px',
                              fontWeight: 600,
                              fontSize: '12px',
                              cursor: 'pointer'
                            }}
                          >
                            Record Recovery
                          </button>
                          <Link
                            to={`/loan-details?loanId=${loan.id}`}
                            className="btn-profile"
                            style={{
                              padding: '6px 10px',
                              backgroundColor: '#F1F5F9',
                              border: '1px solid #CBD5E1',
                              borderRadius: '4px',
                              fontSize: '12px',
                              color: '#475569',
                              textDecoration: 'none'
                            }}
                          >
                            Profile
                          </Link>
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

      {/* Tab 3: Active Loans */}
      {activeTab === 'active' && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden'
        }}>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                  <th style={{ padding: '12px 16px' }}>Loan ID</th>
                  <th style={{ padding: '12px 16px' }}>Borrower</th>
                  <th style={{ padding: '12px 16px' }}>Sanction Amount</th>
                  <th style={{ padding: '12px 16px' }}>Outstanding</th>
                  <th style={{ padding: '12px 16px' }}>Monthly EMI</th>
                  <th style={{ padding: '12px 16px' }}>DPD Status</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {activeLoans.map((loan) => (
                  <tr key={loan.id} style={{ borderBottom: '1px solid #F1F5F9' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0050A4' }}>
                      #{loan.id}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <Link to={`/customer-profile?customerId=${loan.customerId}`} style={{ fontWeight: 700, color: '#0A2E65', textDecoration: 'none' }}>
                        {loan.customerName}
                      </Link>
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 700 }}>
                      {formatCurrency(loan.amount)}
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0050A4' }}>
                      {formatCurrency(loan.remaining)}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {formatCurrency(loan.emi)}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {getDpdBadge(loan.dpd)}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <Link
                        to={`/loan-details?loanId=${loan.id}`}
                        className="btn-profile"
                        style={{
                          padding: '6px 12px',
                          backgroundColor: '#EFF6FF',
                          border: '1px solid #BFDBFE',
                          borderRadius: '4px',
                          fontSize: '12px',
                          color: '#0050A4',
                          fontWeight: 600,
                          textDecoration: 'none'
                        }}
                      >
                        Profile
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 4: Recent Settlements */}
      {activeTab === 'collections' && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden'
        }}>
          {recentCollections.length === 0 ? (
            <div style={{ padding: '40px 20px', textAlign: 'center', color: '#64748B' }}>
              No recent collection transactions recorded.
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                    <th style={{ padding: '12px 16px' }}>Txn Reference</th>
                    <th style={{ padding: '12px 16px' }}>Amount Collected</th>
                    <th style={{ padding: '12px 16px' }}>Mode</th>
                    <th style={{ padding: '12px 16px' }}>Bank UTR</th>
                    <th style={{ padding: '12px 16px' }}>Date</th>
                    <th style={{ padding: '12px 16px' }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentCollections.map((txn, idx) => (
                    <tr key={idx} style={{ borderBottom: '1px solid #F1F5F9' }}>
                      <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0050A4' }}>
                        {txn.transactionId}
                      </td>
                      <td style={{ padding: '14px 16px', fontWeight: 800, color: '#16A34A' }}>
                        {formatCurrency(txn.amount)}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        {txn.paymentMode}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <code>{txn.referenceNumber}</code>
                      </td>
                      <td style={{ padding: '14px 16px', color: '#64748B' }}>
                        {txn.paymentDate}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <span style={{ backgroundColor: '#DCFCE7', color: '#166534', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>
                          {txn.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Review & Sanction Modal */}
      {reviewLoan && (
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
            maxWidth: '560px',
            width: '100%',
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
                  Credit Sanction Decision: Loan #{reviewLoan.id}
                </h3>
                <span style={{ fontSize: '12px', color: '#64748B' }}>
                  Borrower: {reviewLoan.customerName} ({reviewLoan.customerId})
                </span>
              </div>
              <button
                type="button"
                onClick={() => setReviewLoan(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748B' }}
              >
                <X size={20} />
              </button>
            </div>

            <div style={{ padding: '24px' }}>
              <div style={{
                backgroundColor: '#F8FAFC',
                borderRadius: '8px',
                padding: '16px',
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '12px',
                fontSize: '13px',
                marginBottom: '20px'
              }}>
                <div>
                  <span style={{ color: '#64748B', display: 'block' }}>Loan Amount</span>
                  <strong style={{ fontSize: '16px', color: '#0A2E65' }}>{formatCurrency(reviewLoan.amount)}</strong>
                </div>
                <div>
                  <span style={{ color: '#64748B', display: 'block' }}>Monthly EMI</span>
                  <strong style={{ fontSize: '16px', color: '#0050A4' }}>{formatCurrency(reviewLoan.emi)}</strong>
                </div>
                <div>
                  <span style={{ color: '#64748B', display: 'block' }}>Tenure</span>
                  <strong>{reviewLoan.tenureMonths} Months</strong>
                </div>
                <div>
                  <span style={{ color: '#64748B', display: 'block' }}>Interest Rate</span>
                  <strong>{reviewLoan.interestRate}% p.a.</strong>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                  Credit Officer Comments / Rejection Reason (if rejecting)
                </label>
                <textarea
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  placeholder="Enter remarks or grounds for decision..."
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E1',
                    fontSize: '13px',
                    boxSizing: 'border-box'
                  }}
                />
              </div>

              <div style={{
                display: 'flex',
                gap: '12px',
                marginTop: '24px'
              }}>
                <button
                  type="button"
                  onClick={handleApprove}
                  disabled={actionLoading}
                  style={{
                    flex: 1,
                    padding: '12px',
                    backgroundColor: '#16A34A',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    fontWeight: 700,
                    fontSize: '14px',
                    cursor: actionLoading ? 'not-allowed' : 'pointer'
                  }}
                >
                  {actionLoading ? 'Processing...' : 'Approve & Disburse'}
                </button>

                <button
                  type="button"
                  onClick={handleReject}
                  disabled={actionLoading}
                  style={{
                    flex: 1,
                    padding: '12px',
                    backgroundColor: '#DC2626',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    fontWeight: 700,
                    fontSize: '14px',
                    cursor: actionLoading ? 'not-allowed' : 'pointer'
                  }}
                >
                  {actionLoading ? 'Processing...' : 'Reject Application'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Record Collection Modal */}
      {collectLoan && (
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
            maxWidth: '500px',
            width: '100%',
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
                  Record Delinquency Recovery
                </h3>
                <span style={{ fontSize: '12px', color: '#64748B' }}>
                  Loan #{collectLoan.id} • {collectLoan.customerName}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setCollectLoan(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748B' }}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleRecordCollection} style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                  Amount Recovered (₹)
                </label>
                <input
                  type="number"
                  min={1}
                  step={0.01}
                  value={collectAmount}
                  onChange={(e) => setCollectAmount(Number(e.target.value))}
                  required
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E1',
                    fontSize: '16px',
                    fontWeight: 700,
                    boxSizing: 'border-box'
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                  Payment Channel
                </label>
                <select
                  value={collectMode}
                  onChange={(e) => setCollectMode(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E1',
                    fontSize: '14px',
                    boxSizing: 'border-box'
                  }}
                >
                  <option value="UPI">UPI Payment</option>
                  <option value="NEFT">NEFT / RTGS</option>
                  <option value="IMPS">IMPS Immediate Transfer</option>
                  <option value="BRANCH_CASH">Branch Cash Counter</option>
                  <option value="CHEQUE">Cheque / Clearing</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                  Bank Reference / UTR Number
                </label>
                <input
                  type="text"
                  value={collectRef}
                  onChange={(e) => setCollectRef(e.target.value)}
                  required
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E1',
                    fontSize: '14px',
                    boxSizing: 'border-box'
                  }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                  Collection Agent Remarks
                </label>
                <input
                  type="text"
                  value={collectRemarks}
                  onChange={(e) => setCollectRemarks(e.target.value)}
                  placeholder="e.g. Borrower paid full overdue installment"
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E1',
                    fontSize: '14px',
                    boxSizing: 'border-box'
                  }}
                />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                <button
                  type="submit"
                  disabled={actionLoading}
                  style={{
                    flex: 1,
                    padding: '12px',
                    backgroundColor: '#16A34A',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    fontWeight: 700,
                    fontSize: '14px',
                    cursor: actionLoading ? 'not-allowed' : 'pointer'
                  }}
                >
                  {actionLoading ? 'Recording...' : 'Confirm Recovery'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default StaffDashboard;
