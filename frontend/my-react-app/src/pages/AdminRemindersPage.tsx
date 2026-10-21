import React, { useState, useEffect } from 'react';
import { adminApi, SimulateWebhookPayload } from '../api/adminApi';
import {
  Bell,
  Send,
  RefreshCw,
  Mail,
  CheckCircle2,
  AlertTriangle,
  Play,
  Layers,
  Code2,
  X
} from 'lucide-react';
import { LoanReminderLog } from '../types';

export const AdminRemindersPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [demoData, setDemoData] = useState<any>(null);
  const [logs, setLogs] = useState<LoanReminderLog[]>([]);
  const [actionNotice, setActionNotice] = useState<string | null>(null);
  const [triggering, setTriggering] = useState(false);

  // Webhook Simulator State
  const [webhookPayload, setWebhookPayload] = useState<SimulateWebhookPayload>({
    loanId: 1,
    dpd: 1,
    eventType: 'PAYMENT_OVERDUE',
    channel: 'EMAIL'
  });
  const [simulating, setSimulating] = useState(false);

  // HTML Preview state
  const [previewDpd, setPreviewDpd] = useState<number>(1);
  const [previewHtml, setPreviewHtml] = useState<string>('');
  const [loadingPreview, setLoadingPreview] = useState<boolean>(false);

  const fetchDemoData = async () => {
    try {
      const data = await adminApi.getRemindersDemoData();
      setDemoData(data);
      setLogs(data.logs || []);
    } catch (err) {
      console.error('Error fetching reminders demo data:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadPreview = async (dpd: number) => {
    setPreviewDpd(dpd);
    setLoadingPreview(true);
    try {
      const res = await adminApi.getPreviewHtml(webhookPayload.loanId || 1, dpd);
      setPreviewHtml(res.html || res.template || '');
    } catch {
      // Fallback preview
      setPreviewHtml(`
        <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #E2E8F0; border-radius: 8px; max-width: 500px; margin: 0 auto;">
          <h2 style="color: #0050A4;">Bank of India</h2>
          <h3 style="color: ${dpd === 3 ? '#DC2626' : (dpd === 2 ? '#C2410C' : '#0A2E65')};">
            ${dpd === 1 ? 'Friendly Payment Reminder (DPD 1)' : (dpd === 2 ? 'Urgent: Loan EMI Past Due (DPD 2)' : 'FINAL NOTICE: Immediate EMI Settlement Required (DPD 3)')}
          </h3>
          <p>Dear Customer, your EMI of ₹8,720 for Loan Account #1 is past due by ${dpd} day(s). Please clear immediately to prevent credit bureau reporting.</p>
          <a href="/pay?loanId=1" style="display: inline-block; padding: 10px 20px; background-color: #16A34A; color: #FFFFFF; text-decoration: none; border-radius: 4px; font-weight: bold;">Pay EMI Online Now</a>
        </div>
      `);
    } finally {
      setLoadingPreview(false);
    }
  };

  useEffect(() => {
    fetchDemoData();
    loadPreview(1);
  }, []);

  const handleTriggerScheduler = async () => {
    setTriggering(true);
    try {
      const res = await adminApi.triggerReminderScheduler();
      setActionNotice(res.message || 'DPD automated reminder scheduler completed execution.');
      await fetchDemoData();
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Failed to trigger scheduler.');
    } finally {
      setTriggering(false);
    }
  };

  const handleSimulateWebhook = async (e: React.FormEvent) => {
    e.preventDefault();
    setSimulating(true);
    try {
      const res = await adminApi.simulateWebhook(webhookPayload);
      setActionNotice(res.message || `Webhook simulated for Loan #${webhookPayload.loanId} (DPD ${webhookPayload.dpd}).`);
      await fetchDemoData();
      await loadPreview(webhookPayload.dpd);
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Webhook simulation failed.');
    } finally {
      setSimulating(false);
    }
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '32px auto', padding: '0 20px' }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '16px',
        marginBottom: '24px'
      }}>
        <div>
          <span style={{
            fontSize: '11px',
            fontWeight: 700,
            letterSpacing: '1px',
            backgroundColor: '#FFEDD5',
            color: '#C2410C',
            padding: '3px 8px',
            borderRadius: '10px',
            textTransform: 'uppercase'
          }}>
            Automated Collections Engine
          </span>
          <h1 style={{ fontSize: '26px', fontWeight: 800, color: '#0A2E65', margin: '8px 0 4px' }}>
            DPD 1–3 Delinquency Reminders & Webhook Simulator
          </h1>
          <p style={{ fontSize: '14px', color: '#64748B', margin: 0 }}>
            Orchestrate automated borrower reminders at DPD 1, 2, and 3 stages and simulate integration webhooks
          </p>
        </div>

        <button
          type="button"
          onClick={handleTriggerScheduler}
          disabled={triggering}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '12px 20px',
            backgroundColor: '#ED7D16',
            color: '#FFFFFF',
            borderRadius: '6px',
            border: 'none',
            fontWeight: 700,
            fontSize: '14px',
            cursor: triggering ? 'not-allowed' : 'pointer',
            boxShadow: '0 4px 12px rgba(237, 125, 22, 0.35)'
          }}
        >
          <Play size={16} /> {triggering ? 'Triggering Scheduler...' : 'Execute DPD Batch Scheduler'}
        </button>
      </div>

      {actionNotice && (
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
            <span>{actionNotice}</span>
          </div>
          <button
            type="button"
            onClick={() => setActionNotice(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#166534' }}
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* 2-Column: Webhook Simulator & HTML Template Preview */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 1.2fr',
        gap: '24px',
        marginBottom: '28px'
      }} className="reminders-grid">
        {/* Left Column: Webhook Simulator */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          padding: '24px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#0A2E65', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Send size={18} color="#0050A4" /> Delinquency Webhook Dispatcher
          </h3>

          <form onSubmit={handleSimulateWebhook} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                Target Loan Facility ID
              </label>
              <input
                type="number"
                min={1}
                value={webhookPayload.loanId}
                onChange={(e) => setWebhookPayload({ ...webhookPayload, loanId: Number(e.target.value) })}
                required
                style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                Delinquency DPD Threshold
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
                {[1, 2, 3].map((dpd) => (
                  <button
                    key={dpd}
                    type="button"
                    onClick={() => {
                      setWebhookPayload({ ...webhookPayload, dpd });
                      loadPreview(dpd);
                    }}
                    style={{
                      padding: '8px',
                      borderRadius: '6px',
                      border: webhookPayload.dpd === dpd ? '2px solid #ED7D16' : '1px solid #CBD5E1',
                      backgroundColor: webhookPayload.dpd === dpd ? '#FFF7ED' : '#FFFFFF',
                      color: webhookPayload.dpd === dpd ? '#C2410C' : '#475569',
                      fontWeight: 700,
                      fontSize: '13px',
                      cursor: 'pointer'
                    }}
                  >
                    DPD {dpd}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                  Dispatch Channel
                </label>
                <select
                  value={webhookPayload.channel}
                  onChange={(e) => setWebhookPayload({ ...webhookPayload, channel: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '13px', boxSizing: 'border-box' }}
                >
                  <option value="EMAIL">Email Notification</option>
                  <option value="SMS">SMS Gateway</option>
                  <option value="WHATSAPP">WhatsApp Official</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '4px' }}>
                  Event Trigger Type
                </label>
                <select
                  value={webhookPayload.eventType}
                  onChange={(e) => setWebhookPayload({ ...webhookPayload, eventType: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '13px', boxSizing: 'border-box' }}
                >
                  <option value="PAYMENT_OVERDUE">PAYMENT_OVERDUE</option>
                  <option value="PAYMENT_DUE">PAYMENT_DUE</option>
                  <option value="FINAL_NOTICE">FINAL_NOTICE</option>
                </select>
              </div>
            </div>

            <button
              type="submit"
              disabled={simulating}
              style={{
                padding: '12px',
                backgroundColor: '#0050A4',
                color: '#FFFFFF',
                borderRadius: '6px',
                border: 'none',
                fontWeight: 700,
                fontSize: '14px',
                cursor: simulating ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                marginTop: '6px'
              }}
            >
              <Send size={16} /> {simulating ? 'Sending Webhook...' : 'Simulate & Send Notification'}
            </button>
          </form>
        </div>

        {/* Right Column: HTML Template Preview */}
        <div style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '10px',
          border: '1px solid #E2E8F0',
          padding: '24px',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
          display: 'flex',
          flexDirection: 'column'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 800, color: '#0A2E65', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Mail size={18} color="#ED7D16" /> Email Template Preview (DPD {previewDpd})
            </h3>
            <div style={{ display: 'flex', gap: '6px' }}>
              {[1, 2, 3].map((d) => (
                <button
                  key={d}
                  type="button"
                  onClick={() => loadPreview(d)}
                  style={{
                    padding: '4px 10px',
                    borderRadius: '4px',
                    border: '1px solid #CBD5E1',
                    backgroundColor: previewDpd === d ? '#0050A4' : '#FFFFFF',
                    color: previewDpd === d ? '#FFFFFF' : '#475569',
                    fontSize: '11px',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  DPD {d}
                </button>
              ))}
            </div>
          </div>

          <div style={{
            flex: 1,
            minHeight: '260px',
            backgroundColor: '#F8FAFC',
            borderRadius: '8px',
            border: '1px solid #E2E8F0',
            padding: '16px',
            overflowY: 'auto'
          }}>
            {loadingPreview ? (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#64748B' }}>
                Rendering HTML template...
              </div>
            ) : (
              <div dangerouslySetInnerHTML={{ __html: previewHtml }} />
            )}
          </div>
        </div>
      </div>

      {/* Dispatched Notification History */}
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
          <span>Reminder Dispatch Logs (<strong>{logs.length}</strong> dispatches)</span>
          <button
            type="button"
            onClick={fetchDemoData}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#0050A4', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}
          >
            <RefreshCw size={14} /> Refresh
          </button>
        </div>

        {logs.length === 0 ? (
          <div style={{ padding: '40px 20px', textAlign: 'center', color: '#64748B' }}>
            No automated reminder logs recorded yet. Execute the batch scheduler above to trigger dispatches.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '12px' }}>
              <thead>
                <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                  <th style={{ padding: '12px 14px' }}>Dispatched At</th>
                  <th style={{ padding: '12px 14px' }}>Customer Name</th>
                  <th style={{ padding: '12px 14px' }}>Recipient Email</th>
                  <th style={{ padding: '12px 14px' }}>Loan Facility</th>
                  <th style={{ padding: '12px 14px' }}>Stage</th>
                  <th style={{ padding: '12px 14px' }}>Channel</th>
                  <th style={{ padding: '12px 14px' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log, idx) => (
                  <tr key={idx} style={{ borderBottom: '1px solid #F1F5F9' }}>
                    <td style={{ padding: '12px 14px', color: '#64748B', whiteSpace: 'nowrap' }}>
                      {log.dispatchedAt || 'Just now'}
                    </td>
                    <td style={{ padding: '12px 14px', fontWeight: 700, color: '#0A2E65' }}>
                      {log.customerName || 'Borrower'}
                    </td>
                    <td style={{ padding: '12px 14px', color: '#475569' }}>
                      {log.customerEmail}
                    </td>
                    <td style={{ padding: '12px 14px', fontWeight: 700, color: '#0050A4' }}>
                      #{log.loanId}
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      <span style={{
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontSize: '11px',
                        fontWeight: 700,
                        backgroundColor: log.dpd === 1 ? '#FEF9C3' : (log.dpd === 2 ? '#FFEDD5' : '#FEE2E2'),
                        color: log.dpd === 1 ? '#854D0E' : (log.dpd === 2 ? '#C2410C' : '#991B1B')
                      }}>
                        DPD {log.dpd}
                      </span>
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      {log.channel || 'EMAIL'}
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      <span style={{
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontSize: '11px',
                        fontWeight: 700,
                        backgroundColor: '#DCFCE7',
                        color: '#166534'
                      }}>
                        {log.status || 'SENT'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <style>{`
        @media (max-width: 900px) {
          .reminders-grid {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  );
};

export default AdminRemindersPage;
