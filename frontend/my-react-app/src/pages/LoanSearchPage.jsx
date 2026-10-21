import React, { useState, useEffect, useRef } from 'react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import SessionTimeoutModal from '../components/SessionTimeoutModal';
import { fetchAuthStatus } from '../services/authService';
import { searchLoans, fetchSearchConfig } from '../services/loanSearchService';
import { getBackendUrl } from '../utils/apiConfig';

export default function LoanSearchPage() {
  const [authState, setAuthState] = useState({
    authenticated: false,
    user: null,
    csrfToken: '',
    csrfHeader: 'X-CSRF-TOKEN',
    csrfParameter: '_csrf'
  });

  const [authorizedBranch, setAuthorizedBranch] = useState('MAIN');
  const [searchType, setSearchType] = useState('ACCOUNT_NUMBER');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(10);

  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [results, setResults] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [errorMessage, setErrorMessage] = useState('');
  const [infoMessage, setInfoMessage] = useState('');

  // Track csrf values in a ref for callbacks
  const csrfRef = useRef({ token: '', header: 'X-CSRF-TOKEN' });

  // 1. Initial mount: Load auth status and migrate any legacy URL query parameters cleanly
  useEffect(() => {
    let isMounted = true;

    async function init() {
      // Check if URL currently has legacy query parameters (e.g., /loan-search?searchType=NAME&query=Ganga)
      const urlParams = new URLSearchParams(window.location.search);
      const initialType = urlParams.get('searchType') || 'ACCOUNT_NUMBER';
      const initialQuery = urlParams.get('query') || '';

      // Immediately remove sensitive query parameters from the browser URL without page reload
      if (window.location.search) {
        window.history.replaceState({}, document.title, window.location.pathname);
      }

      if (initialQuery) {
        setSearchType(initialType);
        setQuery(initialQuery);
      }

      // Fetch authentication status
      const authData = await fetchAuthStatus();
      if (!isMounted) return;

      if (authData) {
        const token = authData.csrfToken || '';
        const header = authData.csrfHeader || 'X-CSRF-TOKEN';
        csrfRef.current = { token, header };

        setAuthState({
          authenticated: !!authData.authenticated,
          user: authData.authenticated ? {
            name: authData.name,
            email: authData.email,
            role: authData.role
          } : null,
          csrfToken: token,
          csrfHeader: header,
          csrfParameter: authData.csrfParameter || '_csrf'
        });
      }

      // Fetch branch configuration
      const config = await fetchSearchConfig();
      if (isMounted && config && config.authorizedBranch) {
        setAuthorizedBranch(config.authorizedBranch);
      }

      // If a query was migrated from legacy URL, trigger the search via secure POST now
      if (initialQuery) {
        triggerSearch(initialType, initialQuery, 0);
      }
    }

    init();

    return () => {
      isMounted = false;
    };
  }, []);

  const triggerSearch = async (typeToUse, queryToUse, pageToUse) => {
    const trimmed = (queryToUse || '').trim();
    if (!trimmed) {
      setErrorMessage('Please enter a search query.');
      return;
    }

    setLoading(true);
    setErrorMessage('');
    setInfoMessage('');

    const res = await searchLoans(
      { searchType: typeToUse, query: trimmed, page: pageToUse, size },
      csrfRef.current.token,
      csrfRef.current.header
    );

    setLoading(false);
    setHasSearched(true);

    if (res.errorMessage) {
      setErrorMessage(res.errorMessage);
      setResults([]);
      setTotalElements(0);
      setTotalPages(0);
      return;
    }

    if (res.infoMessage) {
      setInfoMessage(res.infoMessage);
    }

    setResults(res.content || []);
    setTotalElements(res.totalElements || 0);
    setTotalPages(res.totalPages || 0);
    setPage(res.page || 0);
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    triggerSearch(searchType, query, 0);
  };

  const handlePageChange = (newPage) => {
    if (newPage < 0 || newPage >= totalPages) return;
    setPage(newPage);
    triggerSearch(searchType, query, newPage);
  };

  const getPlaceholderAndHint = () => {
    switch (searchType) {
      case 'ACCOUNT_NUMBER':
        return {
          placeholder: 'Enter 12-character Account Number (e.g. LN0000000004)',
          maxLength: 12,
          hint: '💡 Exact Loan Account Number format: 2 uppercase letters followed by 10 digits (e.g., LN0000000004).'
        };
      case 'CUSTOMER_ID':
        return {
          placeholder: 'Enter Customer ID (e.g. CUST000001 or 1)',
          maxLength: 25,
          hint: '💡 Secondary search: Exact Customer ID match within your authorized branch.'
        };
      case 'PHONE':
        return {
          placeholder: 'Enter 10-digit mobile number (e.g. 9876543210)',
          maxLength: 15,
          hint: '💡 Secondary search: Registered phone number matching authorized customer accounts.'
        };
      case 'NAME':
        return {
          placeholder: 'Enter customer name (e.g. Rahul Sharma)',
          maxLength: 50,
          hint: '💡 Secondary search: Case-insensitive customer name match within your branch.'
        };
      default:
        return {
          placeholder: 'Enter search query',
          maxLength: 50,
          hint: ''
        };
    }
  };

  const formatCurrency = (val) => {
    if (val == null || isNaN(val)) return '0.00';
    return Number(val).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  const configInfo = getPlaceholderAndHint();

  return (
    <div className="loan-search-page-layout" style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: '#F7F9FC' }}>
      <Navbar authState={authState} />

      <main style={{ flex: 1, padding: '30px 20px', maxWidth: '1300px', margin: '0 auto', width: '100%' }}>
        <div className="wide-card" style={{ background: '#FFFFFF', borderRadius: '12px', padding: '28px', boxShadow: '0 2px 10px rgba(0,0,0,0.06)', border: '1px solid #E2E8F0' }}>
          
          {/* Header row */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
            <div>
              <h2 style={{ margin: 0, color: '#0A2E65', fontSize: '24px', fontWeight: 800 }}>
                🔍 Loan Account & Customer Search
              </h2>
              <p style={{ color: '#64748B', fontSize: '14px', margin: '6px 0 0 0' }}>
                Authorized Bank Staff Portal — Search customer loan accounts and KYC profiles securely.
              </p>
            </div>
            <div>
              <span style={{ background: '#EEF2F6', color: '#1E293B', padding: '8px 16px', borderRadius: '20px', fontSize: '13px', fontWeight: 600, border: '1px solid #CBD5E1' }}>
                🏢 Authorized Branch: <strong>{authorizedBranch}</strong>
              </span>
            </div>
          </div>

          {/* Error Banner */}
          {errorMessage && (
            <div style={{ background: '#FEE2E2', border: '1px solid #FECACA', color: '#B91C1C', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <strong>⚠️ Notice:</strong> {errorMessage}
            </div>
          )}

          {/* Info Banner */}
          {infoMessage && (
            <div style={{ background: '#EFF6FF', border: '1px solid #BFDBFE', color: '#1D4ED8', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>ℹ️</span> {infoMessage}
            </div>
          )}

          {/* Search Form Card (Submits via POST, keeping URL clean) */}
          <div style={{ background: '#F8FAFC', border: '1px solid #E2E8F0', borderRadius: '10px', padding: '22px', marginBottom: '28px' }}>
            <form onSubmit={handleSearchSubmit}>
              <div style={{ display: 'grid', gridTemplateColumns: '230px 1fr 140px', gap: '16px', alignItems: 'end' }}>
                <div>
                  <label htmlFor="searchTypeSelect" style={{ fontWeight: 600, fontSize: '13px', color: '#475569', display: 'block', marginBottom: '6px' }}>
                    Search By Category
                  </label>
                  <select
                    id="searchTypeSelect"
                    value={searchType}
                    onChange={(e) => setSearchType(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', background: '#FFFFFF', fontSize: '14px', color: '#1E293B' }}
                  >
                    <option value="ACCOUNT_NUMBER">Loan Account No</option>
                    <option value="CUSTOMER_ID">Customer ID</option>
                    <option value="PHONE">Registered Mobile</option>
                    <option value="NAME">Customer Name</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="queryInput" style={{ fontWeight: 600, fontSize: '13px', color: '#475569', display: 'block', marginBottom: '6px' }}>
                    Search Query
                  </label>
                  <input
                    id="queryInput"
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder={configInfo.placeholder}
                    maxLength={configInfo.maxLength}
                    required
                    style={{ width: '100%', padding: '10px 14px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '14px', color: '#1E293B', boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <button
                    type="submit"
                    disabled={loading}
                    className="btn"
                    style={{
                      height: '42px',
                      width: '100%',
                      margin: 0,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '8px',
                      background: loading ? '#94A3B8' : 'var(--accent-orange)',
                      cursor: loading ? 'not-allowed' : 'pointer',
                      boxShadow: 'none'
                    }}
                  >
                    <span>{loading ? 'Searching...' : '🔎 Search'}</span>
                  </button>
                </div>
              </div>

              <div style={{ marginTop: '12px', fontSize: '12px', color: '#64748B' }}>
                {configInfo.hint}
              </div>
            </form>
          </div>

          {/* Results Section */}
          {hasSearched && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <h3 style={{ margin: 0, color: '#1E293B', fontSize: '18px', fontWeight: 700 }}>
                  Search Results{' '}
                  {totalElements > 0 && (
                    <span style={{ fontSize: '14px', fontWeight: 'normal', color: '#64748B' }}>
                      (Found {totalElements} matching record{totalElements > 1 ? 's' : ''})
                    </span>
                  )}
                </h3>
              </div>

              {results.length > 0 ? (
                <>
                  <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                      <thead>
                        <tr style={{ background: '#F1F5F9', borderBottom: '2px solid #E2E8F0', color: '#475569' }}>
                          <th style={{ padding: '12px 10px' }}>Account No</th>
                          <th style={{ padding: '12px 10px' }}>Customer ID</th>
                          <th style={{ padding: '12px 10px' }}>Customer Name</th>
                          <th style={{ padding: '12px 10px' }}>Mobile</th>
                          <th style={{ padding: '12px 10px' }}>Branch</th>
                          <th style={{ padding: '12px 10px' }}>Sanctioned</th>
                          <th style={{ padding: '12px 10px' }}>Outstanding</th>
                          <th style={{ padding: '12px 10px' }}>Repayment</th>
                          <th style={{ padding: '12px 10px' }}>DPD</th>
                          <th style={{ padding: '12px 10px' }}>Status</th>
                          <th style={{ padding: '12px 10px', textAlign: 'center' }}>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {results.map((row) => (
                          <tr key={row.loanId || row.accountNumber} style={{ borderBottom: '1px solid #E2E8F0' }}>
                            <td style={{ padding: '12px 10px' }}>
                              <strong style={{ color: '#0050A4', fontFamily: 'monospace', fontSize: '14px' }}>
                                {row.accountNumber}
                              </strong>
                            </td>
                            <td style={{ padding: '12px 10px', fontFamily: 'monospace', color: '#475569' }}>
                              {row.customerId}
                            </td>
                            <td style={{ padding: '12px 10px', fontWeight: 600, color: '#1E293B' }}>
                              {row.customerName}
                            </td>
                            <td style={{ padding: '12px 10px', color: '#475569' }}>
                              {row.maskedPhone || '—'}
                            </td>
                            <td style={{ padding: '12px 10px' }}>
                              <span style={{ background: '#EEF2F6', color: '#475569', padding: '3px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 600 }}>
                                {row.branch}
                              </span>
                            </td>
                            <td style={{ padding: '12px 10px' }}>₹{formatCurrency(row.amount)}</td>
                            <td style={{ padding: '12px 10px', fontWeight: 700, color: row.remaining > 0 ? '#C53030' : '#16A34A' }}>
                              ₹{formatCurrency(row.remaining)}
                            </td>
                            <td style={{ padding: '12px 10px' }}>
                              {row.repaymentStatus === 'OVERDUE' ? (
                                <span style={{ color: '#9B2C2C', background: '#FED7D7', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 700 }}>
                                  ⚠️ OVERDUE {row.overdueAmount > 0 ? `(₹${Math.round(row.overdueAmount).toLocaleString('en-IN')})` : ''}
                                </span>
                              ) : row.repaymentStatus === 'CURRENT' ? (
                                <span style={{ color: '#22543D', background: '#C6F6D5', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600 }}>
                                  CURRENT
                                </span>
                              ) : row.repaymentStatus === 'CLOSED' ? (
                                <span style={{ color: '#475569', background: '#E2E8F0', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600 }}>
                                  CLOSED
                                </span>
                              ) : (
                                <span style={{ color: '#744210', background: '#FEEBC8', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600 }}>
                                  {row.repaymentStatus}
                                </span>
                              )}
                            </td>
                            <td style={{ padding: '12px 10px' }}>
                              {row.dpd > 0 ? (
                                <span style={{ color: '#742A2A', background: '#FED7D7', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 700 }}>
                                  {row.dpd} DPD
                                </span>
                              ) : row.status === 'CLOSED' ? (
                                <span style={{ color: '#94A3B8', fontSize: '12px' }}>-</span>
                              ) : (
                                <span style={{ color: '#22543D', background: '#C6F6D5', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600 }}>
                                  0 DPD
                                </span>
                              )}
                            </td>
                            <td style={{ padding: '12px 10px' }}>
                              {row.status === 'ACTIVE' ? (
                                <span style={{ color: '#16A34A', background: '#DCFCE7', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600 }}>
                                  ACTIVE
                                </span>
                              ) : row.status === 'CLOSED' ? (
                                <span style={{ color: '#475569', background: '#E2E8F0', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600 }}>
                                  CLOSED
                                </span>
                              ) : (
                                <span style={{ color: '#B7791F', background: '#FEEBC8', padding: '3px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600 }}>
                                  {row.status}
                                </span>
                              )}
                            </td>
                            <td style={{ padding: '12px 10px', textAlign: 'center' }}>
                              <a
                                href={getBackendUrl(`/loan-details?loanId=${row.loanId}`)}
                                style={{
                                  display: 'inline-block',
                                  textDecoration: 'none',
                                  textAlign: 'center',
                                  padding: '6px 14px',
                                  fontSize: '12px',
                                  background: '#0050A4',
                                  color: '#FFFFFF',
                                  fontWeight: 600,
                                  borderRadius: '6px',
                                  transition: 'background 0.15s ease'
                                }}
                              >
                                Profile
                              </a>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Pagination without modifying URL query string */}
                  {totalPages > 1 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px', paddingTop: '15px', borderTop: '1px solid #EDF2F7' }}>
                      <div style={{ fontSize: '13px', color: '#64748B' }}>
                        Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>
                      </div>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          type="button"
                          onClick={() => handlePageChange(page - 1)}
                          disabled={page === 0}
                          style={{
                            padding: '6px 14px',
                            fontSize: '13px',
                            background: '#FFFFFF',
                            color: page === 0 ? '#CBD5E1' : '#0050A4',
                            border: '1.5px solid #E2E8F0',
                            borderRadius: '6px',
                            cursor: page === 0 ? 'not-allowed' : 'pointer'
                          }}
                        >
                          &laquo; Previous
                        </button>
                        <button
                          type="button"
                          onClick={() => handlePageChange(page + 1)}
                          disabled={page >= totalPages - 1}
                          style={{
                            padding: '6px 14px',
                            fontSize: '13px',
                            background: '#FFFFFF',
                            color: page >= totalPages - 1 ? '#CBD5E1' : '#0050A4',
                            border: '1.5px solid #E2E8F0',
                            borderRadius: '6px',
                            cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer'
                          }}
                        >
                          Next &raquo;
                        </button>
                      </div>
                    </div>
                  )}
                </>
              ) : (
                !errorMessage && (
                  <div style={{ background: '#FFFDF5', border: '1px solid #FEEBC8', padding: '28px', borderRadius: '8px', textAlign: 'center', color: '#744210', marginTop: '10px' }}>
                    <p style={{ fontSize: '16px', fontWeight: 600, margin: '0 0 6px 0' }}>No Loan Account Records Match Your Criteria</p>
                    <p style={{ fontSize: '13px', color: '#975A16', margin: 0 }}>
                      Please verify the input and ensure the account belongs to your authorized branch.
                    </p>
                  </div>
                )
              )}
            </div>
          )}
        </div>
      </main>

      <Footer />
      <SessionTimeoutModal authState={authState} />
    </div>
  );
}
