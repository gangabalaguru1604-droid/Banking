import React, { useState } from 'react';
import { initialStaff } from '../services/mockData';

export default function AdminStaffPage() {
  const [staffList, setStaffList] = useState(initialStaff);
  const [showModal, setShowModal] = useState(false);
  const [newStaffName, setNewStaffName] = useState('');
  const [newStaffEmail, setNewStaffEmail] = useState('');
  const [newStaffRole, setNewStaffRole] = useState('ROLE_LOAN_OFFICER');
  const [newStaffDept, setNewStaffDept] = useState('Underwriting');

  const handleAddStaff = (e) => {
    e.preventDefault();
    const newMember = {
      id: staffList.length + 1,
      name: newStaffName,
      email: newStaffEmail,
      role: newStaffRole,
      department: newStaffDept,
      status: 'ACTIVE',
      createdDate: new Date().toISOString().split('T')[0]
    };
    setStaffList(prev => [...prev, newMember]);
    setShowModal(false);
    setNewStaffName('');
    setNewStaffEmail('');
    alert(`Staff member ${newMember.name} provisioned successfully!`);
  };

  const getRoleBadge = (role) => {
    switch (role) {
      case 'ROLE_LOAN_OFFICER': return { label: 'Loan Officer', color: '#60a5fa', bg: 'rgba(59,130,246,0.2)' };
      case 'ROLE_COLLECTIONS_AGENT': return { label: 'Collections Agent', color: '#f59e0b', bg: 'rgba(245,158,11,0.2)' };
      case 'ROLE_COLLECTIONS_MANAGER': return { label: 'Collections Manager', color: '#a78bfa', bg: 'rgba(124,58,237,0.2)' };
      case 'ROLE_ADMIN': return { label: 'Admin', color: '#f87171', bg: 'rgba(239,68,68,0.2)' };
      default: return { label: role, color: '#fff', bg: 'rgba(255,255,255,0.1)' };
    }
  };

  return (
    <div className="container" style={{ padding: '40px 20px 80px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: '28px', fontWeight: 800, color: '#fff' }}>
            👥 Staff Account & Role Management
          </h1>
          <p style={{ color: '#94a3b8', fontSize: '14px', marginTop: '4px' }}>
            Administer institutional permissions for credit officers, collections agents, and managers
          </p>
        </div>

        <button 
          type="button" 
          onClick={() => setShowModal(true)} 
          className="btn-primary" 
          style={{ padding: '10px 20px', fontSize: '13px' }}
        >
          + Provision New Staff Account
        </button>
      </div>

      <div className="glass-panel" style={{ padding: '30px' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', color: '#64748b', fontSize: '12px', textTransform: 'uppercase' }}>
                <th style={{ padding: '12px 16px' }}>ID</th>
                <th style={{ padding: '12px 16px' }}>Staff Name</th>
                <th style={{ padding: '12px 16px' }}>Institutional Email</th>
                <th style={{ padding: '12px 16px' }}>Assigned Role</th>
                <th style={{ padding: '12px 16px' }}>Department</th>
                <th style={{ padding: '12px 16px' }}>Status</th>
                <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {staffList.map(s => {
                const badge = getRoleBadge(s.role);
                return (
                  <tr key={s.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#cbd5e1' }}>
                    <td style={{ padding: '16px', fontWeight: 700 }}>#{s.id}</td>
                    <td style={{ padding: '16px', fontWeight: 600, color: '#fff' }}>{s.name}</td>
                    <td style={{ padding: '16px', color: '#60a5fa' }}>{s.email}</td>
                    <td style={{ padding: '16px' }}>
                      <span style={{ background: badge.bg, color: badge.color, padding: '4px 10px', borderRadius: '8px', fontSize: '11px', fontWeight: 800 }}>
                        {badge.label}
                      </span>
                    </td>
                    <td style={{ padding: '16px' }}>{s.department}</td>
                    <td style={{ padding: '16px' }}>
                      <span style={{ color: '#34d399', fontWeight: 700, fontSize: '12px' }}>✓ ACTIVE</span>
                    </td>
                    <td style={{ padding: '16px', textAlign: 'right' }}>
                      <button 
                        type="button" 
                        onClick={() => alert(`Reset MFA requested for ${s.name}`)}
                        style={{ background: 'transparent', border: '1px solid rgba(255,255,255,0.1)', color: '#94a3b8', padding: '4px 8px', borderRadius: '6px', fontSize: '11px', cursor: 'pointer' }}
                      >
                        Reset 2FA
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Provision Modal */}
      {showModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(8px)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '480px', padding: '32px' }}>
            <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '20px', fontWeight: 800, color: '#fff', marginBottom: '18px' }}>
              Provision New Staff Account
            </h3>

            <form onSubmit={handleAddStaff}>
              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>Staff Full Name</label>
                <input 
                  type="text" 
                  value={newStaffName} 
                  onChange={(e) => setNewStaffName(e.target.value)} 
                  placeholder="e.g. Ramesh Kulkarni" 
                  required 
                  style={{ width: '100%', padding: '10px 14px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
                />
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>Bank Email</label>
                <input 
                  type="email" 
                  value={newStaffEmail} 
                  onChange={(e) => setNewStaffEmail(e.target.value)} 
                  placeholder="e.g. ramesh.officer@bank.com" 
                  required 
                  style={{ width: '100%', padding: '10px 14px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
                />
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>Staff Role</label>
                <select 
                  value={newStaffRole} 
                  onChange={(e) => setNewStaffRole(e.target.value)}
                  style={{ width: '100%', padding: '10px 14px', background: '#070b14', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
                >
                  <option value="ROLE_LOAN_OFFICER">Loan Officer (Origination & Approvals)</option>
                  <option value="ROLE_COLLECTIONS_AGENT">Collections Agent (DPD Overdue Recording)</option>
                  <option value="ROLE_COLLECTIONS_MANAGER">Collections Manager (Overdue Oversight)</option>
                  <option value="ROLE_ADMIN">System Administrator</option>
                </select>
              </div>

              <div style={{ marginBottom: '24px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>Department</label>
                <input 
                  type="text" 
                  value={newStaffDept} 
                  onChange={(e) => setNewStaffDept(e.target.value)} 
                  placeholder="e.g. Retail Credit Underwriting" 
                  required 
                  style={{ width: '100%', padding: '10px 14px', background: 'rgba(7, 11, 20, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '8px', color: '#fff', fontSize: '13px', outline: 'none' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button type="button" onClick={() => setShowModal(false)} className="btn-secondary" style={{ padding: '8px 16px' }}>Cancel</button>
                <button type="submit" className="btn-primary" style={{ padding: '8px 20px' }}>Provision Account</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
