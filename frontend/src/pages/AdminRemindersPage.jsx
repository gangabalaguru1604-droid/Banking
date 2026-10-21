import React, { useState } from 'react';
import { initialReminders } from '../services/mockData';

export default function AdminRemindersPage() {
  const [reminders, setReminders] = useState(initialReminders);
  const [running, setRunning] = useState(false);

  const formatINR = (val) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);

  const handleTriggerSimulation = () => {
    setRunning(true);
    setTimeout(() => {
      setRunning(false);
      const newSim = {
        id: `REM-${Date.now().toString().slice(-4)}`,
        loanId: 102,
        customerName: 'Suresh Patil',
        dpd: 2,
        overdueAmount: 6959.00,
        penalty: 150.00,
        totalDue: 7109.00,
        channel: 'SMS & Email',
        status: 'DELIVERED',
        dispatchTime: new Date().toLocaleTimeString()
      };
      setReminders(prev => [newSim, ...prev]);
      alert('DPD Reminder Batch dispatched successfully! 1 overdue SMS and Email notification sent.');
    }, 800);
  };

  return (
    <div className="container" style={{ padding: '40px 20px 80px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#fff' }}>
            🔔 Automated DPD Reminder & Penalty Scheduler
          </h1>
          <p style={{ color: '#94a3b8', fontSize: '14px', marginTop: '4px' }}>
            Automated multi-channel notification engine (SMS & HTTPS Email) for delinquent borrower accounts
          </p>
        </div>

        <button 
          type="button" 
          onClick={handleTriggerSimulation} 
          disabled={running}
          className="btn-primary" 
          style={{ padding: '10px 20px', fontSize: '13px' }}
        >
          {running ? 'Dispatching Batch...' : '▶ Run DPD Batch Simulation'}
        </button>
      </div>

      {/* DPD Penalty Tier Rules */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px', marginBottom: '32px' }}>
        <div className="glass-panel" style={{ padding: '24px', borderLeft: '4px solid #60a5fa' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 700 }}>Stage 1 — Grace Notice</div>
          <div style={{ fontFamily: 'var(--font-heading)', fontSize: '22px', fontWeight: 800, color: '#fff', marginTop: '4px' }}>
            DPD 1 (1 Day Overdue)
          </div>
          <div style={{ fontSize: '14px', color: '#34d399', fontWeight: 700, marginTop: '8px' }}>
            Penalty: ₹0.00 (Zero Penalty Grace)
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            Channel: Automated SMS Reminder
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '24px', borderLeft: '4px solid #f59e0b' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 700 }}>Stage 2 — First Late Fee</div>
          <div style={{ fontFamily: 'var(--font-heading)', fontSize: '22px', fontWeight: 800, color: '#fff', marginTop: '4px' }}>
            DPD 2 (2 Days Overdue)
          </div>
          <div style={{ fontSize: '14px', color: '#f59e0b', fontWeight: 700, marginTop: '8px' }}>
            Penalty: ₹150.00 Fixed Late Fee
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            Channel: SMS + Transactional Email
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '24px', borderLeft: '4px solid #f87171' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 700 }}>Stage 3 — Escalation Notice</div>
          <div style={{ fontFamily: 'var(--font-heading)', fontSize: '22px', fontWeight: 800, color: '#fff', marginTop: '4px' }}>
            DPD 3 (3+ Days Overdue)
          </div>
          <div style={{ fontSize: '14px', color: '#f87171', fontWeight: 700, marginTop: '8px' }}>
            Penalty: ₹300.00 Maximum Penalty
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            Channel: Escalation to Field Collections
          </div>
        </div>
      </div>

      {/* Dispatched Notification History */}
      <div className="glass-panel" style={{ padding: '30px' }}>
        <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '18px', fontWeight: 700, color: '#fff', marginBottom: '20px' }}>
          Recent Notification Dispatch Logs
        </h3>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>
                <th style={{ padding: '12px 16px' }}>Dispatch ID</th>
                <th style={{ padding: '12px 16px' }}>Borrower</th>
                <th style={{ padding: '12px 16px' }}>DPD Stage</th>
                <th style={{ padding: '12px 16px' }}>Overdue Base</th>
                <th style={{ padding: '12px 16px' }}>Late Penalty</th>
                <th style={{ padding: '12px 16px' }}>Total Amount</th>
                <th style={{ padding: '12px 16px' }}>Channels</th>
                <th style={{ padding: '12px 16px', textAlign: 'right' }}>Delivery Status</th>
              </tr>
            </thead>
            <tbody>
              {reminders.map(rem => (
                <tr key={rem.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#cbd5e1' }}>
                  <td style={{ padding: '16px', fontFamily: 'monospace', fontWeight: 700, color: '#60a5fa' }}>{rem.id}</td>
                  <td style={{ padding: '16px', fontWeight: 600, color: '#fff' }}>{rem.customerName}</td>
                  <td style={{ padding: '16px' }}>
                    <span style={{ background: 'rgba(239,68,68,0.2)', color: '#f87171', padding: '3px 8px', borderRadius: '6px', fontSize: '11px', fontWeight: 700 }}>
                      DPD {rem.dpd}
                    </span>
                  </td>
                  <td style={{ padding: '16px' }}>{formatINR(rem.overdueAmount)}</td>
                  <td style={{ padding: '16px', color: '#f59e0b', fontWeight: 700 }}>{formatINR(rem.penalty)}</td>
                  <td style={{ padding: '16px', color: '#fff', fontWeight: 800 }}>{formatINR(rem.totalDue)}</td>
                  <td style={{ padding: '16px', fontSize: '13px' }}>{rem.channel}</td>
                  <td style={{ padding: '16px', textAlign: 'right' }}>
                    <span style={{ color: '#34d399', fontWeight: 700, fontSize: '12px' }}>✓ {rem.status}</span>
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
