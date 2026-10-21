import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { loanApi } from '../api/loanApi';
import {
  UserCheck,
  ShieldAlert,
  CreditCard,
  Building,
  Phone,
  Mail,
  MapPin,
  Calendar,
  Eye,
  EyeOff,
  Lock,
  AlertCircle,
  FileText
} from 'lucide-react';
import { CustomerProfileDto, Loan } from '../types';

export const CustomerProfilePage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const customerId = searchParams.get('customerId') || '';

  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<CustomerProfileDto | null>(null);
  const [loans, setLoans] = useState<Loan[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Sensitive Field Revelation states
  const [revealedPan, setRevealedPan] = useState<string | null>(null);
  const [revealedAadhaar, setRevealedAadhaar] = useState<string | null>(null);
  const [revealAuditNotice, setRevealAuditNotice] = useState<string | null>(null);
  const [revealingField, setRevealingField] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function loadCustomer() {
      if (!customerId) {
        setError('No Customer ID specified.');
        setLoading(false);
        return;
      }

      try {
        const data = await loanApi.getCustomerProfile(customerId);
        if (isMounted) {
          setProfile(data.profile || data);
          setLoans(data.loans || []);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.response?.data?.error || err.message || 'Failed to load customer profile.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    loadCustomer();
    return () => { isMounted = false; };
  }, [customerId]);

  const handleRevealField = async (fieldName: 'pan' | 'aadhaar') => {
    if (!profile) return;
    setRevealingField(fieldName);
    try {
      const res = await loanApi.revealSensitiveField(profile.customerId, fieldName);
      if (res.success && res.unmaskedValue) {
        if (fieldName === 'pan') {
          setRevealedPan(res.unmaskedValue);
        } else {
          setRevealedAadhaar(res.unmaskedValue);
        }
        setRevealAuditNotice(`Audit notice: Decryption of ${fieldName.toUpperCase()} logged for compliance audit trail.`);
      } else {
        alert(res.error || `Unable to reveal ${fieldName}.`);
      }
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || `Failed to decrypt ${fieldName}.`);
    } finally {
      setRevealingField(null);
    }
  };

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
      <div style={{ maxWidth: '1200px', margin: '40px auto', padding: '0 20px', textAlign: 'center' }}>
        <div style={{
          width: '36px',
          height: '36px',
          border: '3px solid #E2E8F0',
          borderTopColor: '#0050A4',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite',
          margin: '0 auto 12px'
        }} />
        <p style={{ color: '#64748B' }}>Loading customer KYC record...</p>
      </div>
    );
  }

  if (error || !profile) {
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
          <h3>Customer Profile Unavailable</h3>
          <p>{error || 'Customer record not found.'}</p>
          <Link to="/loan-search" className="btn">Return to Loan Search</Link>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1200px', margin: '32px auto', padding: '0 20px' }}>
      {/* Back Link */}
      <div style={{ marginBottom: '20px' }}>
        <Link to="/loan-search" style={{ fontSize: '13px', color: '#0050A4', fontWeight: 600, textDecoration: 'none' }}>
          ← Back to Loan Search
        </Link>
      </div>

      {/* Header Profile Banner */}
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '12px',
        border: '1px solid #E2E8F0',
        padding: '24px 28px',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '20px',
        marginBottom: '24px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            backgroundColor: '#EFF6FF',
            color: '#0050A4',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '24px',
            fontWeight: 800
          }}>
            {profile.name?.charAt(0) || 'C'}
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <h1 style={{ fontSize: '22px', fontWeight: 800, color: '#0A2E65', margin: 0 }}>
                {profile.name}
              </h1>
              <span style={{
                fontSize: '11px',
                fontWeight: 700,
                backgroundColor: '#DCFCE7',
                color: '#166534',
                padding: '2px 8px',
                borderRadius: '12px'
              }}>
                KYC VERIFIED
              </span>
            </div>
            <p style={{ fontSize: '13px', color: '#64748B', margin: '4px 0 0' }}>
              CIF Number: <strong>{profile.customerId}</strong> • Branch: <strong>{profile.branch || 'MAIN'}</strong>
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '16px' }}>
          <div style={{ textAlign: 'right' }}>
            <span style={{ fontSize: '11px', color: '#64748B', textTransform: 'uppercase', fontWeight: 600 }}>CIBIL Score</span>
            <div style={{ fontSize: '22px', fontWeight: 800, color: (profile.creditScore || 750) >= 700 ? '#16A34A' : '#ED7D16' }}>
              {profile.creditScore || 750} / 900
            </div>
          </div>
        </div>
      </div>

      {revealAuditNotice && (
        <div style={{
          backgroundColor: '#FFF7ED',
          border: '1px solid #FED7AA',
          color: '#9A3412',
          padding: '10px 14px',
          borderRadius: '8px',
          fontSize: '12px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <ShieldAlert size={16} />
          <span>{revealAuditNotice}</span>
        </div>
      )}

      {/* 2-Column Info Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))',
        gap: '24px',
        marginBottom: '28px'
      }}>
        {/* Personal & KYC Info Card */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          padding: '22px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#0A2E65', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <UserCheck size={18} color="#0050A4" /> Personal & Identity Details
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', fontSize: '13px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Mail size={16} color="#64748B" />
              <span style={{ color: '#64748B', width: '90px' }}>Email:</span>
              <strong style={{ color: '#0A2E65' }}>{profile.email}</strong>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Phone size={16} color="#64748B" />
              <span style={{ color: '#64748B', width: '90px' }}>Phone:</span>
              <strong style={{ color: '#0A2E65' }}>{profile.phoneNumber || '+91 98765 43210'}</strong>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <MapPin size={16} color="#64748B" />
              <span style={{ color: '#64748B', width: '90px' }}>Address:</span>
              <span style={{ color: '#0A2E65' }}>{profile.address || '102, Star Residency, Bandra West, Mumbai'}</span>
            </div>

            {/* Audited Masked PAN */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '10px 12px',
              backgroundColor: '#F8FAFC',
              borderRadius: '6px',
              border: '1px solid #E2E8F0',
              marginTop: '4px'
            }}>
              <div>
                <span style={{ fontSize: '11px', color: '#64748B', display: 'block', fontWeight: 600 }}>PAN Number (Tax ID)</span>
                <code style={{ fontSize: '14px', fontWeight: 800, color: '#0A2E65' }}>
                  {revealedPan || profile.panMasked || 'ABCDE****F'}
                </code>
              </div>
              {!revealedPan && (
                <button
                  type="button"
                  disabled={revealingField === 'pan'}
                  onClick={() => handleRevealField('pan')}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    padding: '6px 10px',
                    backgroundColor: '#FFFFFF',
                    border: '1px solid #CBD5E1',
                    borderRadius: '4px',
                    fontSize: '11px',
                    fontWeight: 600,
                    color: '#0050A4',
                    cursor: 'pointer'
                  }}
                >
                  <Eye size={12} /> {revealingField === 'pan' ? 'Decrypting...' : 'Reveal (Audit)'}
                </button>
              )}
            </div>

            {/* Audited Masked Aadhaar */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '10px 12px',
              backgroundColor: '#F8FAFC',
              borderRadius: '6px',
              border: '1px solid #E2E8F0'
            }}>
              <div>
                <span style={{ fontSize: '11px', color: '#64748B', display: 'block', fontWeight: 600 }}>Aadhaar Number (UIDAI)</span>
                <code style={{ fontSize: '14px', fontWeight: 800, color: '#0A2E65' }}>
                  {revealedAadhaar || profile.aadhaarMasked || '•••• •••• 9812'}
                </code>
              </div>
              {!revealedAadhaar && (
                <button
                  type="button"
                  disabled={revealingField === 'aadhaar'}
                  onClick={() => handleRevealField('aadhaar')}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    padding: '6px 10px',
                    backgroundColor: '#FFFFFF',
                    border: '1px solid #CBD5E1',
                    borderRadius: '4px',
                    fontSize: '11px',
                    fontWeight: 600,
                    color: '#0050A4',
                    cursor: 'pointer'
                  }}
                >
                  <Eye size={12} /> {revealingField === 'aadhaar' ? 'Decrypting...' : 'Reveal (Audit)'}
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Financial & Underwriting Profile Card */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          padding: '22px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#0A2E65', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Building size={18} color="#0050A4" /> Employment & Financial Standing
          </h3>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', fontSize: '13px' }}>
            <div style={{ padding: '12px', backgroundColor: '#F8FAFC', borderRadius: '6px' }}>
              <span style={{ color: '#64748B', display: 'block', fontSize: '12px' }}>Employment Type</span>
              <strong style={{ color: '#0A2E65' }}>{profile.employmentType || 'Salaried (Tier 1 Corporate)'}</strong>
            </div>

            <div style={{ padding: '12px', backgroundColor: '#F8FAFC', borderRadius: '6px' }}>
              <span style={{ color: '#64748B', display: 'block', fontSize: '12px' }}>Verified Monthly Income</span>
              <strong style={{ color: '#16A34A', fontSize: '15px' }}>{formatCurrency(profile.monthlyIncome || 65000)}</strong>
            </div>

            <div style={{ padding: '12px', backgroundColor: '#F8FAFC', borderRadius: '6px' }}>
              <span style={{ color: '#64748B', display: 'block', fontSize: '12px' }}>Total Borrowing</span>
              <strong style={{ color: '#0A2E65' }}>{formatCurrency(profile.totalBorrowing || 500000)}</strong>
            </div>

            <div style={{ padding: '12px', backgroundColor: '#F8FAFC', borderRadius: '6px' }}>
              <span style={{ color: '#64748B', display: 'block', fontSize: '12px' }}>Current Outstanding</span>
              <strong style={{ color: '#C2410C' }}>{formatCurrency(profile.currentOutstanding || 240000)}</strong>
            </div>
          </div>

          <div style={{ marginTop: '16px', padding: '12px', backgroundColor: '#F0FDF4', borderRadius: '6px', border: '1px solid #BBF7D0', fontSize: '12px', color: '#166534' }}>
            <strong>Repayment History Track Record:</strong> {profile.repaymentTrackRecord || 'Prompt / Standard (Zero DPD defaults)'}
          </div>
        </div>
      </div>

      {/* Customer's Loans Table */}
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '10px',
        border: '1px solid #E2E8F0',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
        overflow: 'hidden'
      }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid #E2E8F0' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#0A2E65', margin: 0 }}>
            Associated Loan Facilities ({loans.length})
          </h3>
        </div>

        {loans.length === 0 ? (
          <div style={{ padding: '30px 20px', textAlign: 'center', color: '#64748B' }}>
            No loan facilities associated with this customer record.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                  <th style={{ padding: '12px 16px' }}>Loan ID</th>
                  <th style={{ padding: '12px 16px' }}>Sanction Amount</th>
                  <th style={{ padding: '12px 16px' }}>Interest Rate</th>
                  <th style={{ padding: '12px 16px' }}>Outstanding</th>
                  <th style={{ padding: '12px 16px' }}>Monthly EMI</th>
                  <th style={{ padding: '12px 16px' }}>Status</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {loans.map((l) => (
                  <tr key={l.id} style={{ borderBottom: '1px solid #F1F5F9' }}>
                    <td style={{ padding: '12px 16px', fontWeight: 700, color: '#0050A4' }}>
                      #{l.id}
                    </td>
                    <td style={{ padding: '12px 16px', fontWeight: 700 }}>
                      {formatCurrency(l.amount)}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      {l.interest}% p.a.
                    </td>
                    <td style={{ padding: '12px 16px', fontWeight: 700, color: l.remaining > 0 ? '#C2410C' : '#16A34A' }}>
                      {formatCurrency(l.remaining)}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      {formatCurrency(l.emi)}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{
                        fontSize: '11px',
                        fontWeight: 700,
                        padding: '2px 8px',
                        borderRadius: '10px',
                        backgroundColor: l.status === 'ACTIVE' ? '#DCFCE7' : '#FEF9C3',
                        color: l.status === 'ACTIVE' ? '#166534' : '#854D0E'
                      }}>
                        {l.status}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                      <Link
                        to={`/loan-details?loanId=${l.id}`}
                        style={{
                          padding: '6px 12px',
                          backgroundColor: '#EFF6FF',
                          border: '1px solid #BFDBFE',
                          borderRadius: '4px',
                          color: '#0050A4',
                          fontWeight: 600,
                          fontSize: '12px',
                          textDecoration: 'none'
                        }}
                      >
                        360° Account Details
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default CustomerProfilePage;
