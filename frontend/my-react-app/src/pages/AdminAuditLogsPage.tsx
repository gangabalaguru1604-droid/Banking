import React, { useState, useEffect } from 'react';
import { adminApi, AuditLogParams } from '../api/adminApi';
import {
  FileText,
  Search,
  Filter,
  RefreshCw,
  ShieldCheck,
  AlertTriangle,
  Clock,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import { AuthAuditLog } from '../types';

export const AdminAuditLogsPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [logs, setLogs] = useState<AuthAuditLog[]>([]);
  const [usernameFilter, setUsernameFilter] = useState('');
  const [eventTypeFilter, setEventTypeFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 15;

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const params: AuditLogParams = {
        page,
        size: pageSize,
        username: usernameFilter.trim() || undefined,
        eventType: eventTypeFilter !== 'ALL' ? eventTypeFilter : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined
      };

      const res = await adminApi.getAuditLogs(params);
      if (res.content) {
        setLogs(res.content);
        setTotalPages(res.totalPages || 1);
        setTotalElements(res.totalElements || res.content.length);
      } else if (Array.isArray(res)) {
        setLogs(res);
        setTotalPages(Math.ceil(res.length / pageSize));
        setTotalElements(res.length);
      } else if (res.logs) {
        setLogs(res.logs);
        setTotalPages(Math.ceil(res.logs.length / pageSize));
        setTotalElements(res.logs.length);
      }
    } catch (err) {
      console.error('Failed to load audit logs:', err);
      setLogs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [page, eventTypeFilter, statusFilter]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchLogs();
  };

  const getStatusBadge = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'SUCCESS':
        return <span style={{ backgroundColor: '#DCFCE7', color: '#166534', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>SUCCESS</span>;
      case 'FAILURE':
        return <span style={{ backgroundColor: '#FEE2E2', color: '#991B1B', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>FAILURE</span>;
      case 'WARNING':
        return <span style={{ backgroundColor: '#FEF9C3', color: '#854D0E', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>WARNING</span>;
      default:
        return <span style={{ backgroundColor: '#F1F5F9', color: '#475569', padding: '2px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 700 }}>{status}</span>;
    }
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '32px auto', padding: '0 20px' }}>
      {/* Page Header */}
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 800, color: '#0A2E65', margin: '0 0 6px' }}>
          Compliance & Security Audit Trail
        </h1>
        <p style={{ fontSize: '14px', color: '#64748B', margin: 0 }}>
          Immutable system audit log tracking logins, MFA events, PDF downloads, and audited KYC field decrypts
        </p>
      </div>

      {/* Filter Card */}
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '10px',
        border: '1px solid #E2E8F0',
        padding: '18px 24px',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
        marginBottom: '24px'
      }}>
        <form onSubmit={handleFilterSubmit} style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr)) 120px',
          gap: '14px',
          alignItems: 'flex-end'
        }}>
          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              Filter by User Email
            </label>
            <input
              type="text"
              value={usernameFilter}
              onChange={(e) => setUsernameFilter(e.target.value)}
              placeholder="e.g. rahul.sharma@example.com"
              style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '13px', boxSizing: 'border-box' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              Audit Event Type
            </label>
            <select
              value={eventTypeFilter}
              onChange={(e) => { setEventTypeFilter(e.target.value); setPage(0); }}
              style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '13px' }}
            >
              <option value="ALL">All Event Types</option>
              <option value="LOGIN_SUCCESS">LOGIN_SUCCESS</option>
              <option value="LOGIN_FAILURE">LOGIN_FAILURE</option>
              <option value="MFA_ENROLLMENT_SUCCESS">MFA_ENROLLMENT_SUCCESS</option>
              <option value="MFA_VERIFICATION_SUCCESS">MFA_VERIFICATION_SUCCESS</option>
              <option value="STATEMENT_DOWNLOAD_SUCCESS">STATEMENT_DOWNLOAD_SUCCESS</option>
              <option value="STATEMENT_DOWNLOAD_DENIED">STATEMENT_DOWNLOAD_DENIED</option>
              <option value="SENSITIVE_FIELD_REVEAL">SENSITIVE_FIELD_REVEAL</option>
              <option value="STAFF_ROLE_CHANGE">STAFF_ROLE_CHANGE</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
              Execution Status
            </label>
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '13px' }}
            >
              <option value="ALL">All Statuses</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="FAILURE">FAILURE</option>
              <option value="WARNING">WARNING</option>
            </select>
          </div>

          <button
            type="submit"
            style={{
              padding: '9px 16px',
              backgroundColor: '#0050A4',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '6px',
              fontWeight: 700,
              fontSize: '13px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px'
            }}
          >
            <Search size={14} /> Filter
          </button>
        </form>
      </div>

      {/* Logs Table */}
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
          <span>Showing <strong>{logs.length}</strong> of <strong>{totalElements}</strong> total audit records</span>
          <button
            type="button"
            onClick={fetchLogs}
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
            <p style={{ color: '#64748B', fontSize: '13px' }}>Querying audit repository...</p>
          </div>
        ) : logs.length === 0 ? (
          <div style={{ padding: '60px 20px', textAlign: 'center', color: '#64748B' }}>
            No audit records matching criteria.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '12px' }}>
              <thead>
                <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                  <th style={{ padding: '12px 14px' }}>Timestamp</th>
                  <th style={{ padding: '12px 14px' }}>Subject User</th>
                  <th style={{ padding: '12px 14px' }}>Event Type</th>
                  <th style={{ padding: '12px 14px' }}>Status</th>
                  <th style={{ padding: '12px 14px' }}>IP Address</th>
                  <th style={{ padding: '12px 14px' }}>Audit Details</th>
                  <th style={{ padding: '12px 14px' }}>Trigger / Admin</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id} style={{ borderBottom: '1px solid #F1F5F9' }}>
                    <td style={{ padding: '12px 14px', whiteSpace: 'nowrap', color: '#64748B' }}>
                      {log.timestamp}
                    </td>
                    <td style={{ padding: '12px 14px', fontWeight: 700, color: '#0A2E65' }}>
                      {log.username}
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      <code style={{ fontSize: '11px', color: '#0050A4', backgroundColor: '#EFF6FF', padding: '2px 6px', borderRadius: '4px' }}>
                        {log.eventType}
                      </code>
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      {getStatusBadge(log.status)}
                    </td>
                    <td style={{ padding: '12px 14px', color: '#64748B' }}>
                      <code>{log.ipAddress || '127.0.0.1'}</code>
                    </td>
                    <td style={{ padding: '12px 14px', color: '#334155' }}>
                      {log.details || log.failureReason || '—'}
                    </td>
                    <td style={{ padding: '12px 14px', color: '#64748B' }}>
                      {log.adminUser || 'System'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
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

export default AdminAuditLogsPage;
