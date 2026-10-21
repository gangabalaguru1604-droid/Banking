import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { loanApi, SearchLoansParams } from '../api/loanApi';
import { useAuth } from '../context/AuthContext';
import {
  Search,
  Filter,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  CreditCard,
  UserCheck,
  Eye,
  RefreshCw
} from 'lucide-react';
import { StaffDashboardLoanDto } from '../types';

export const LoanSearchPage: React.FC = () => {
  const { authState } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Search filter states
  const [query, setQuery] = useState(searchParams.get('query') || '');
  const [branch, setBranch] = useState(searchParams.get('branch') || 'ALL');
  const [status, setStatus] = useState(searchParams.get('status') || 'ALL');
  const [dpdBucket, setDpdBucket] = useState(searchParams.get('dpdBucket') || 'ALL');
  const [sortBy, setSortBy] = useState('appliedDate');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const [loading, setLoading] = useState(true);
  const [results, setResults] = useState<StaffDashboardLoanDto[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchResults = async () => {
    setLoading(true);
    try {
      const params: SearchLoansParams = {
        query: query.trim() || undefined,
        branch: branch !== 'ALL' ? branch : undefined,
        status: status !== 'ALL' ? status : undefined,
        dpdBucket: dpdBucket !== 'ALL' ? dpdBucket : undefined,
        sortBy,
        sortDir,
        page,
        size: pageSize
      };

      const res = await loanApi.searchLoans(params);
      if (res.content) {
        setResults(res.content);
        setTotalCount(res.totalElements || res.content.length);
        setTotalPages(res.totalPages || Math.ceil((res.totalElements || res.content.length) / pageSize));
      } else if (Array.isArray(res)) {
        setResults(res);
        setTotalCount(res.length);
        setTotalPages(Math.ceil(res.length / pageSize));
      } else if (res.loans) {
        setResults(res.loans);
        setTotalCount(res.loans.length);
        setTotalPages(Math.ceil(res.loans.length / pageSize));
      }
    } catch (err) {
      console.error('Error fetching loan search results:', err);
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchResults();
  }, [page, branch, status, dpdBucket, sortBy, sortDir]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchResults();
  };

  const formatCurrency = (val: number | undefined) => {
    if (val === undefined || val === null) return '₹0.00';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val);
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

  return (
    <div style={{ maxWidth: '1280px', margin: '32px auto', padding: '0 20px' }}>
      {/* Header */}
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 800, color: '#0A2E65', margin: '0 0 6px' }}>
          Multi-Criteria Loan & Delinquency Search
        </h1>
        <p style={{ fontSize: '14px', color: '#64748B', margin: 0 }}>
          Search borrower portfolios by Loan ID, CIF/Customer ID, Branch, or RBI DPD Stage
        </p>
      </div>

      {/* Filter Card */}
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '10px',
        border: '1px solid #E2E8F0',
        padding: '20px 24px',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
        marginBottom: '24px'
      }}>
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Search bar row */}
          <div style={{ display: 'flex', gap: '12px' }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <div style={{ position: 'absolute', left: '12px', top: '12px', color: '#94A3B8' }}>
                <Search size={18} />
              </div>
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search by Loan ID, Borrower Name, Email, or Customer ID..."
                style={{
                  width: '100%',
                  padding: '10px 14px 10px 40px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '14px',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <button
              type="submit"
              style={{
                padding: '10px 20px',
                backgroundColor: '#0050A4',
                color: '#FFFFFF',
                border: 'none',
                borderRadius: '6px',
                fontWeight: 700,
                fontSize: '14px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
            >
              <Search size={16} /> Search
            </button>
          </div>

          {/* Filters row */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: '14px',
            paddingTop: '12px',
            borderTop: '1px solid #F1F5F9'
          }}>
            {/* Branch */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                Branch Jurisdiction
              </label>
              <select
                value={branch}
                onChange={(e) => { setBranch(e.target.value); setPage(0); }}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '13px'
                }}
              >
                <option value="ALL">All Branches</option>
                <option value="MAIN">MAIN Branch</option>
                <option value="SOUTH">SOUTH Branch</option>
                <option value="NORTH">NORTH Branch</option>
                <option value="EAST">EAST Branch</option>
                <option value="WEST">WEST Branch</option>
              </select>
            </div>

            {/* Status */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                Loan Status
              </label>
              <select
                value={status}
                onChange={(e) => { setStatus(e.target.value); setPage(0); }}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '13px'
                }}
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="PENDING">PENDING</option>
                <option value="CLOSED">CLOSED</option>
                <option value="REJECTED">REJECTED</option>
              </select>
            </div>

            {/* DPD Bucket */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                Delinquency DPD Stage
              </label>
              <select
                value={dpdBucket}
                onChange={(e) => { setDpdBucket(e.target.value); setPage(0); }}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '13px'
                }}
              >
                <option value="ALL">All DPD Stages</option>
                <option value="CURRENT">Current (0 DPD)</option>
                <option value="SMA_0">SMA-0 (1-30 DPD)</option>
                <option value="SMA_1">SMA-1 (31-60 DPD)</option>
                <option value="SMA_2">SMA-2 (61-90 DPD)</option>
                <option value="NPA">NPA (&gt;90 DPD)</option>
              </select>
            </div>

            {/* Sort by */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                Sort By
              </label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                style={{
                  width: '100%',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E1',
                  fontSize: '13px'
                }}
              >
                <option value="appliedDate">Applied Date</option>
                <option value="amount">Sanction Amount</option>
                <option value="remaining">Outstanding Balance</option>
                <option value="dpd">DPD Days</option>
                <option value="id">Loan ID</option>
              </select>
            </div>
          </div>
        </form>
      </div>

      {/* Results Table */}
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '10px',
        border: '1px solid #E2E8F0',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
        overflow: 'hidden'
      }}>
        <div style={{
          padding: '16px 20px',
          borderBottom: '1px solid #E2E8F0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          fontSize: '13px',
          color: '#64748B'
        }}>
          <span>Showing <strong>{results.length}</strong> of <strong>{totalCount}</strong> matching loan records</span>
          <button
            type="button"
            onClick={fetchResults}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#0050A4', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}
          >
            <RefreshCw size={14} /> Refresh
          </button>
        </div>

        {loading ? (
          <div style={{ padding: '60px 20px', textAlign: 'center' }}>
            <div style={{
              width: '32px',
              height: '32px',
              border: '3px solid #E2E8F0',
              borderTopColor: '#0050A4',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite',
              margin: '0 auto 12px'
            }} />
            <p style={{ color: '#64748B', fontSize: '13px' }}>Executing loan search query...</p>
          </div>
        ) : results.length === 0 ? (
          <div style={{ padding: '60px 20px', textAlign: 'center', color: '#64748B' }}>
            <Search size={36} color="#94A3B8" style={{ margin: '0 auto 8px' }} />
            <p>No loan accounts found matching your search criteria.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                  <th style={{ padding: '12px 16px' }}>Loan ID</th>
                  <th style={{ padding: '12px 16px' }}>Customer Name / CIF</th>
                  <th style={{ padding: '12px 16px' }}>Branch</th>
                  <th style={{ padding: '12px 16px' }}>Sanction Amount</th>
                  <th style={{ padding: '12px 16px' }}>Outstanding</th>
                  <th style={{ padding: '12px 16px' }}>EMI</th>
                  <th style={{ padding: '12px 16px' }}>Status</th>
                  <th style={{ padding: '12px 16px' }}>DPD Stage</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {results.map((loan) => (
                  <tr
                    key={loan.id}
                    style={{ borderBottom: '1px solid #F1F5F9' }}
                    onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#F8FAFC')}
                    onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                  >
                    <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0050A4' }}>
                      <Link to={`/loan-details?loanId=${loan.id}`} style={{ textDecoration: 'none', color: '#0050A4' }}>
                        #{loan.id}
                      </Link>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <Link to={`/customer-profile?customerId=${loan.customerId}`} style={{ fontWeight: 700, color: '#0A2E65', textDecoration: 'none' }}>
                        {loan.customerName}
                      </Link>
                      <div style={{ fontSize: '11px', color: '#64748B' }}>{loan.customerId}</div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {loan.branch}
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 700 }}>
                      {formatCurrency(loan.amount)}
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 700, color: loan.remaining > 0 ? '#C2410C' : '#16A34A' }}>
                      {formatCurrency(loan.remaining)}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {formatCurrency(loan.emi)}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{
                        fontSize: '11px',
                        fontWeight: 700,
                        padding: '2px 8px',
                        borderRadius: '10px',
                        backgroundColor: loan.status === 'ACTIVE' ? '#DCFCE7' : (loan.status === 'CLOSED' ? '#E0F2FE' : '#FEF9C3'),
                        color: loan.status === 'ACTIVE' ? '#166534' : (loan.status === 'CLOSED' ? '#075985' : '#854D0E')
                      }}>
                        {loan.status}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {getDpdBadge(loan.dpd)}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '8px' }}>
                        <Link
                          to={`/loan-details?loanId=${loan.id}`}
                          className="btn-profile"
                          style={{
                            padding: '6px 10px',
                            backgroundColor: '#EFF6FF',
                            border: '1px solid #BFDBFE',
                            borderRadius: '4px',
                            color: '#0050A4',
                            fontSize: '12px',
                            fontWeight: 600,
                            textDecoration: 'none',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '4px'
                          }}
                        >
                          <Eye size={13} /> Profile
                        </Link>
                        <Link
                          to={`/customer-profile?customerId=${loan.customerId}`}
                          style={{
                            padding: '6px 10px',
                            backgroundColor: '#F1F5F9',
                            border: '1px solid #CBD5E1',
                            borderRadius: '4px',
                            color: '#475569',
                            fontSize: '12px',
                            fontWeight: 600,
                            textDecoration: 'none'
                          }}
                        >
                          KYC
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div style={{
            padding: '14px 20px',
            borderTop: '1px solid #E2E8F0',
            backgroundColor: '#F8FAFC',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
                padding: '6px 12px',
                backgroundColor: '#FFFFFF',
                border: '1px solid #CBD5E1',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: page === 0 ? 'not-allowed' : 'pointer',
                opacity: page === 0 ? 0.6 : 1
              }}
            >
              <ChevronLeft size={14} /> Previous
            </button>

            <span style={{ fontSize: '13px', color: '#64748B' }}>
              Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>
            </span>

            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
                padding: '6px 12px',
                backgroundColor: '#FFFFFF',
                border: '1px solid #CBD5E1',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
                opacity: page >= totalPages - 1 ? 0.6 : 1
              }}
            >
              Next <ChevronRight size={14} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default LoanSearchPage;
