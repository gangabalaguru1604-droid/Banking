import React, { useState } from 'react';
import { initialAudits } from '../services/mockData';

export default function AdminAuditsPage() {
  const [audits, setAudits] = useState(initialAudits);
  const [search, setSearch] = useState('');

  const filtered = audits.filter(a => 
    !search ||
    a.user.toLowerCase().includes(search.toLowerCase()) ||
    a.event.toLowerCase().includes(search.toLowerCase()) ||
    a.action.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="container" style={{ padding: '40px 20px 80px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#fff' }}>
            🔒 Institutional Security & Audit Trail
          </h1>
          <p style={{ color: '#94a3b8', fontSize: '14px', marginTop: '4px' }}>
            Immutable event logs for regulatory compliance (RBI / ISO 27001 standard)
          </p>
        </div>

        <input 
          type="text" 
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Filter audit events or user..."
          style={{ width: '280px', padding: '10px 14px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '10px', color: '#fff', fontSize: '13px', outline: 'none' }}
        />
      </div>

      <div className="glass-panel" style={{ padding: '30px' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>
                <th style={{ padding: '12px 16px' }}>Audit ID</th>
                <th style={{ padding: '12px 16px' }}>Timestamp</th>
                <th style={{ padding: '12px 16px' }}>Actor / User</th>
                <th style={{ padding: '12px 16px' }}>Event Category</th>
                <th style={{ padding: '12px 16px' }}>Action Executed</th>
                <th style={{ padding: '12px 16px' }}>Client IP</th>
                <th style={{ padding: '12px 16px', textAlign: 'right' }}>Outcome</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(audit => (
                <tr key={audit.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#cbd5e1' }}>
                  <td style={{ padding: '16px', fontFamily: 'monospace', fontWeight: 700, color: '#60a5fa' }}>{audit.id}</td>
                  <td style={{ padding: '16px', fontSize: '13px', color: '#94a3b8' }}>{audit.timestamp}</td>
                  <td style={{ padding: '16px', fontWeight: 600, color: '#fff' }}>{audit.user}</td>
                  <td style={{ padding: '16px' }}>
                    <span style={{ background: 'rgba(59,130,246,0.15)', color: '#93c5fd', padding: '3px 8px', borderRadius: '6px', fontSize: '11px', fontWeight: 700 }}>
                      {audit.event}
                    </span>
                  </td>
                  <td style={{ padding: '16px', fontFamily: 'monospace', fontSize: '13px' }}>{audit.action}</td>
                  <td style={{ padding: '16px', fontFamily: 'monospace', fontSize: '12px', color: '#64748b' }}>{audit.ip}</td>
                  <td style={{ padding: '16px', textAlign: 'right' }}>
                    <span style={{ color: '#34d399', fontWeight: 800, fontSize: '12px' }}>✓ SUCCESS</span>
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
