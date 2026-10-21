import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { loanApi } from '../api/loanApi';
import { useAuth } from '../context/AuthContext';
import { StatCard } from '../components/StatCard';
import {
  FileText,
  Download,
  Building,
  ShieldCheck,
  CreditCard,
  Calendar,
  AlertCircle,
  Clock,
  CheckCircle2,
  DollarSign,
  Layers,
  ArrowRight
} from 'lucide-react';
import { LoanAccountDetailsDTO } from '../types';

export const Loan360DetailsPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const loanId = searchParams.get('loanId') || '';
  const { authState } = useAuth();

  const [loading, setLoading] = useState(true);
  const [loan, setLoan] = useState<LoanAccountDetailsDTO | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'schedule' | 'cbs' | 'collateral' | 'history'>('schedule');

  useEffect(() => {
    let isMounted = true;
    async function loadLoanDetails() {
      if (!loanId) {
        setError('No Loan Account ID provided.');
        setLoading(false);
        return;
      }

      try {
        const data = await loanApi.getLoanAccountDetails(loanId);
        if (isMounted) {
          setLoan(data.loanDetails || data);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.response?.data?.error || err.message || 'Failed to load loan account details.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    loadLoanDetails();
    return () => { isMounted = false; };
  }, [loanId]);

  const formatCurrency = (val: number | undefined) => {
    if (val === undefined || val === null) return '₹0.00';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val);
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '1280px', margin: '40px auto', padding: '0 20px', textAlign: 'center' }}>
        <div style={{
          width: '36px',
          height: '36px',
          border: '3px solid #E2E8F0',
          borderTopColor: '#0050A4',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite',
          margin: '0 auto 12px'
        }} />
        <p style={{ color: '#64748B' }}>Fetching 360° Loan Account Details from Core Banking...</p>
      </div>
    );
  }

  if (error || !loan) {
    return (
      <div style={{ maxWidth: '800px', margin: '40px auto', padding: '0 20px' }}>
        <div style={{
          backgroundColor: '#FEF2F2',
          border: '1px solid #FEE2E2',
          color: '#991B1B',
          padding: '20px',
          borderRadius: '8px',
          textAlign: 'center'
        }}>
          <AlertCircle size={32} style={{ margin: '0 auto 8px', color: '#DC2626' }} />
          <h3>Loan Record Unavailable</h3>
          <p>{error || 'Account details could not be retrieved.'}</p>
          <Link to="/loan-search" className="btn">Back to Loan Search</Link>
        </div>
      </div>
    );
  }

  const statementPdfUrl = loanApi.getPdfStatementUrl(loan.id);

  return (
    <div style={{ maxWidth: '1280px', margin: '32px auto', padding: '0 20px' }}>
      {/* Top Navigation */}
      <div style={{ marginBottom: '20px' }}>
        <Link to="/loan-search" style={{ fontSize: '13px', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
          ← Back to Loan Search
        </Link>
      </div>

      {/* Account Master Banner */}
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
        marginBottom: '28px',
        boxShadow: '0 4px 12px rgba(10, 46, 101, 0.15)'
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{
              fontSize: '11px',
              fontWeight: 700,
              letterSpacing: '1px',
              backgroundColor: '#0050A4',
              padding: '3px 8px',
              borderRadius: '10px',
              textTransform: 'uppercase'
            }}>
              Facility #{loan.id}
            </span>
            <span style={{
              fontSize: '11px',
              fontWeight: 700,
              backgroundColor: loan.status === 'ACTIVE' ? '#16A34A' : '#ED7D16',
              padding: '3px 8px',
              borderRadius: '10px',
              textTransform: 'uppercase'
            }}>
              {loan.status}
            </span>
          </div>

          <h1 style={{ fontSize: '24px', fontWeight: 800, margin: '8px 0 4px' }}>
            {loan.customerName}
          </h1>

          <p style={{ fontSize: '13px', color: '#94A3B8', margin: 0 }}>
            CIF ID: <strong>{loan.customerId}</strong> • Branch: <strong>{loan.branch || 'MAIN'}</strong> • Applied: <strong>{loan.appliedDate}</strong>
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <a
            href={statementPdfUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '10px 18px',
              backgroundColor: '#ED7D16',
              color: '#FFFFFF',
              borderRadius: '6px',
              textDecoration: 'none',
              fontWeight: 700,
              fontSize: '13px',
              boxShadow: '0 4px 12px rgba(237, 125, 22, 0.3)'
            }}
          >
            <Download size={16} /> Download Statement (PDF)
          </a>
        </div>
      </div>

      {/* 4 Key Stat Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
        gap: '20px',
        marginBottom: '28px'
      }}>
        <StatCard
          title="Sanction Amount"
          value={formatCurrency(loan.sanctionAmount)}
          subtitle={`${loan.interestRate}% p.a. • ${loan.tenureMonths} Mo Tenure`}
          icon={<DollarSign size={20} />}
          color="#0050A4"
        />
        <StatCard
          title="Outstanding Balance"
          value={formatCurrency(loan.remainingAmount)}
          subtitle={`Monthly EMI: ${formatCurrency(loan.emi)}`}
          icon={<CreditCard size={20} />}
          color="#ED7D16"
        />
        <StatCard
          title="Total Paid to Date"
          value={formatCurrency(loan.totalPaid || 0)}
          subtitle="Cumulative principal + interest recovered"
          icon={<CheckCircle2 size={20} />}
          color="#16A34A"
        />
        <StatCard
          title="Delinquency Status"
          value={loan.dpd === 0 ? 'Current (0 DPD)' : `${loan.dpdBucket} (${loan.dpd} DPD)`}
          subtitle={loan.overdueAmount > 0 ? `Overdue: ${formatCurrency(loan.overdueAmount)}` : 'Zero Overdue'}
          icon={<Clock size={20} />}
          color={loan.dpd === 0 ? '#16A34A' : '#DC2626'}
        />
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
          onClick={() => setActiveTab('schedule')}
          style={{
            padding: '10px 18px',
            border: 'none',
            borderBottom: activeTab === 'schedule' ? '3px solid #0050A4' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: activeTab === 'schedule' ? '#0050A4' : '#64748B',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          Amortization Schedule ({(loan.installments || []).length})
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('cbs')}
          style={{
            padding: '10px 18px',
            border: 'none',
            borderBottom: activeTab === 'cbs' ? '3px solid #0050A4' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: activeTab === 'cbs' ? '#0050A4' : '#64748B',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          CBS Core Account
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('collateral')}
          style={{
            padding: '10px 18px',
            border: 'none',
            borderBottom: activeTab === 'collateral' ? '3px solid #0050A4' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: activeTab === 'collateral' ? '#0050A4' : '#64748B',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          Collateral & Security
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('history')}
          style={{
            padding: '10px 18px',
            border: 'none',
            borderBottom: activeTab === 'history' ? '3px solid #0050A4' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: activeTab === 'history' ? '#0050A4' : '#64748B',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          Payment Ledger ({(loan.transactions || []).length})
        </button>
      </div>

      {/* Tab 1: Amortization Schedule */}
      {activeTab === 'schedule' && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden'
        }}>
          {(!loan.installments || loan.installments.length === 0) ? (
            <div style={{ padding: '40px 20px', textAlign: 'center', color: '#64748B' }}>
              No installments generated yet for this facility.
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                    <th style={{ padding: '12px 16px' }}>Inst #</th>
                    <th style={{ padding: '12px 16px' }}>Due Date</th>
                    <th style={{ padding: '12px 16px' }}>Principal Due</th>
                    <th style={{ padding: '12px 16px' }}>Interest Due</th>
                    <th style={{ padding: '12px 16px' }}>Total EMI Due</th>
                    <th style={{ padding: '12px 16px' }}>Amount Paid</th>
                    <th style={{ padding: '12px 16px' }}>Status</th>
                    <th style={{ padding: '12px 16px' }}>DPD Days</th>
                  </tr>
                </thead>
                <tbody>
                  {loan.installments.map((inst) => (
                    <tr key={inst.installmentNumber} style={{ borderBottom: '1px solid #F1F5F9' }}>
                      <td style={{ padding: '12px 16px', fontWeight: 700, color: '#0050A4' }}>
                        #{inst.installmentNumber}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        {inst.dueDate}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        {formatCurrency(inst.principalDue)}
                      </td>
                      <td style={{ padding: '12px 16px', color: '#475569' }}>
                        {formatCurrency(inst.interestDue)}
                      </td>
                      <td style={{ padding: '12px 16px', fontWeight: 700, color: '#0A2E65' }}>
                        {formatCurrency(inst.totalDue)}
                      </td>
                      <td style={{ padding: '12px 16px', fontWeight: 600, color: inst.totalPaid > 0 ? '#16A34A' : '#94A3B8' }}>
                        {formatCurrency(inst.totalPaid)}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span style={{
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: '10px',
                          backgroundColor: inst.status === 'PAID' ? '#DCFCE7' : (inst.dpd > 0 ? '#FEE2E2' : '#FEF9C3'),
                          color: inst.status === 'PAID' ? '#166534' : (inst.dpd > 0 ? '#991B1B' : '#854D0E')
                        }}>
                          {inst.status}
                        </span>
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        {inst.dpd > 0 ? (
                          <strong style={{ color: '#DC2626' }}>{inst.dpd} DPD</strong>
                        ) : (
                          <span style={{ color: '#16A34A' }}>0</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: CBS Core Account */}
      {activeTab === 'cbs' && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          padding: '24px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#0A2E65', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Building size={18} color="#0050A4" /> Core Banking System (CBS) Linked Operative Account
          </h3>

          {loan.cbsAccount ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', fontSize: '13px' }}>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Account Number</span>
                <strong style={{ fontSize: '16px', color: '#0A2E65' }}>{loan.cbsAccount.accountNumber}</strong>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Account Type</span>
                <strong style={{ color: '#0A2E65' }}>{loan.cbsAccount.accountType}</strong>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Current Balance</span>
                <strong style={{ fontSize: '16px', color: '#16A34A' }}>{formatCurrency(loan.cbsAccount.balance)}</strong>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>IFSC Code</span>
                <code>{loan.cbsAccount.ifscCode}</code>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Lien Marked</span>
                <strong style={{ color: '#C2410C' }}>{formatCurrency(loan.cbsAccount.lienAmount)}</strong>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Account Status</span>
                <span style={{ backgroundColor: '#DCFCE7', color: '#166534', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>
                  {loan.cbsAccount.accountStatus}
                </span>
              </div>
            </div>
          ) : (
            <div style={{ padding: '20px', color: '#64748B' }}>No linked CBS operative account on record.</div>
          )}
        </div>
      )}

      {/* Tab 3: Collateral & Security */}
      {activeTab === 'collateral' && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          padding: '24px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#0A2E65', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ShieldCheck size={18} color="#0050A4" /> Security & Asset Collateral Valuation
          </h3>

          {loan.collateral ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', fontSize: '13px' }}>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Collateral Category</span>
                <strong style={{ fontSize: '15px', color: '#0A2E65' }}>{loan.collateral.collateralType}</strong>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Valuation Amount</span>
                <strong style={{ fontSize: '16px', color: '#16A34A' }}>{formatCurrency(loan.collateral.valuationAmount)}</strong>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>LTV (Loan-to-Value) Ratio</span>
                <strong style={{ fontSize: '16px', color: '#0050A4' }}>{loan.collateral.ltvRatio}%</strong>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Document Reference</span>
                <code>{loan.collateral.documentReference}</code>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Verification Status</span>
                <span style={{ backgroundColor: '#DCFCE7', color: '#166534', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>
                  {loan.collateral.verificationStatus}
                </span>
              </div>
              <div style={{ padding: '14px', backgroundColor: '#F8FAFC', borderRadius: '8px' }}>
                <span style={{ color: '#64748B', display: 'block' }}>Valuation Date</span>
                <span>{loan.collateral.valuationDate}</span>
              </div>
            </div>
          ) : (
            <div style={{ padding: '20px', color: '#64748B' }}>Unsecured retail facility (No physical collateral registered).</div>
          )}
        </div>
      )}

      {/* Tab 4: Ledger History */}
      {activeTab === 'history' && (
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden'
        }}>
          {(!loan.transactions || loan.transactions.length === 0) ? (
            <div style={{ padding: '40px 20px', textAlign: 'center', color: '#64748B' }}>
              No repayment transactions recorded for this facility yet.
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                <thead>
                  <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                    <th style={{ padding: '12px 16px' }}>Txn ID</th>
                    <th style={{ padding: '12px 16px' }}>Date</th>
                    <th style={{ padding: '12px 16px' }}>Amount Paid</th>
                    <th style={{ padding: '12px 16px' }}>Channel</th>
                    <th style={{ padding: '12px 16px' }}>Bank UTR / Reference</th>
                    <th style={{ padding: '12px 16px' }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {loan.transactions.map((t, idx) => (
                    <tr key={idx} style={{ borderBottom: '1px solid #F1F5F9' }}>
                      <td style={{ padding: '12px 16px', fontWeight: 700, color: '#0050A4' }}>
                        {t.transactionId}
                      </td>
                      <td style={{ padding: '12px 16px', color: '#64748B' }}>
                        {t.paymentDate}
                      </td>
                      <td style={{ padding: '12px 16px', fontWeight: 800, color: '#16A34A' }}>
                        {formatCurrency(t.amount)}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        {t.paymentMode}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <code>{t.referenceNumber}</code>
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span style={{ backgroundColor: '#DCFCE7', color: '#166534', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>
                          {t.status}
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
    </div>
  );
};

export default Loan360DetailsPage;
