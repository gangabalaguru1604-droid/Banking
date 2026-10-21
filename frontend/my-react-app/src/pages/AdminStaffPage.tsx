import React, { useState, useEffect } from 'react';
import { adminApi, CreateStaffPayload } from '../api/adminApi';
import {
  Users,
  UserPlus,
  Shield,
  KeyRound,
  Unlock,
  CheckCircle2,
  AlertCircle,
  X,
  Search,
  Lock
} from 'lucide-react';
import { StaffUser } from '../types';

export const AdminStaffPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [staffList, setStaffList] = useState<StaffUser[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);

  // Modal: Provision staff
  const [showAddModal, setShowAddModal] = useState(false);
  const [newStaff, setNewStaff] = useState<CreateStaffPayload>({
    name: '',
    email: '',
    password: '',
    role: 'ROLE_LOAN_OFFICER',
    branch: 'MAIN'
  });
  const [saving, setSaving] = useState(false);

  // Modal: Update role
  const [selectedUser, setSelectedUser] = useState<StaffUser | null>(null);
  const [targetRole, setTargetRole] = useState<string>('ROLE_LOAN_OFFICER');

  const fetchStaff = async () => {
    try {
      const data = await adminApi.getStaffList();
      setStaffList(data.staff || (Array.isArray(data) ? data : []));
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load staff list.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStaff();
  }, []);

  const handleCreateStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newStaff.name || !newStaff.email || !newStaff.password) {
      alert('Please fill in all required fields.');
      return;
    }

    setSaving(true);
    try {
      const res = await adminApi.createStaff(newStaff);
      if (res.success) {
        setSuccessNotice(`Staff user ${newStaff.email} provisioned successfully.`);
        setShowAddModal(false);
        setNewStaff({
          name: '',
          email: '',
          password: '',
          role: 'ROLE_LOAN_OFFICER',
          branch: 'MAIN'
        });
        await fetchStaff();
      } else {
        alert(res.error || 'Failed to create staff.');
      }
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Failed to provision staff.');
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateRole = async () => {
    if (!selectedUser) return;
    setSaving(true);
    try {
      const res = await adminApi.updateStaffRole(selectedUser.id, targetRole);
      if (res.success) {
        setSuccessNotice(`Role updated for ${selectedUser.email} to ${targetRole}.`);
        setSelectedUser(null);
        await fetchStaff();
      } else {
        alert(res.error || 'Role update failed.');
      }
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Role update failed.');
    } finally {
      setSaving(false);
    }
  };

  const handleResetMfa = async (user: StaffUser) => {
    if (!window.confirm(`Are you sure you want to reset 2FA TOTP for ${user.email}? They will be prompted to re-enroll on their next login.`)) {
      return;
    }
    try {
      const res = await adminApi.resetStaffMfa(user.id);
      if (res.success) {
        setSuccessNotice(`MFA secret reset for ${user.email}. User can now re-enroll.`);
        await fetchStaff();
      } else {
        alert(res.error || 'MFA reset failed.');
      }
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Failed to reset MFA.');
    }
  };

  const handleUnlockAccount = async (user: StaffUser) => {
    try {
      const res = await adminApi.unlockStaff(user.id);
      if (res.success) {
        setSuccessNotice(`Account ${user.email} unlocked successfully.`);
        await fetchStaff();
      } else {
        alert(res.error || 'Failed to unlock account.');
      }
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || 'Failed to unlock account.');
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
          <h1 style={{ fontSize: '26px', fontWeight: 800, color: '#0A2E65', margin: '0 0 6px' }}>
            Staff & Role-Based Access Management
          </h1>
          <p style={{ fontSize: '14px', color: '#64748B', margin: 0 }}>
            Provision banking officers, assign RBAC permissions, manage MFA resets, and unlock accounts
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowAddModal(true)}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '12px 20px',
            backgroundColor: '#0050A4',
            color: '#FFFFFF',
            borderRadius: '6px',
            border: 'none',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(0, 80, 164, 0.25)'
          }}
        >
          <UserPlus size={18} /> Provision New Staff
        </button>
      </div>

      {successNotice && (
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
            <span>{successNotice}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessNotice(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#166534' }}
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* Staff Table */}
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '10px',
        border: '1px solid #E2E8F0',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
        overflow: 'hidden'
      }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid #E2E8F0', fontSize: '13px', color: '#64748B' }}>
          Total Officers: <strong>{staffList.length}</strong>
        </div>

        {loading ? (
          <div style={{ padding: '50px 20px', textAlign: 'center' }}>
            <div style={{
              width: '32px',
              height: '32px',
              border: '3px solid #E2E8F0',
              borderTopColor: '#0050A4',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite',
              margin: '0 auto 12px'
            }} />
            <p style={{ color: '#64748B', fontSize: '13px' }}>Loading staff directory...</p>
          </div>
        ) : staffList.length === 0 ? (
          <div style={{ padding: '50px 20px', textAlign: 'center', color: '#64748B' }}>
            No staff records found.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0', color: '#475569' }}>
                  <th style={{ padding: '12px 16px' }}>Officer Name</th>
                  <th style={{ padding: '12px 16px' }}>Email Address</th>
                  <th style={{ padding: '12px 16px' }}>Role</th>
                  <th style={{ padding: '12px 16px' }}>Branch</th>
                  <th style={{ padding: '12px 16px' }}>2FA Status</th>
                  <th style={{ padding: '12px 16px' }}>Security State</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>Admin Operations</th>
                </tr>
              </thead>
              <tbody>
                {staffList.map((user) => (
                  <tr key={user.id} style={{ borderBottom: '1px solid #F1F5F9' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 700, color: '#0A2E65' }}>
                      {user.name}
                    </td>
                    <td style={{ padding: '14px 16px', color: '#475569' }}>
                      {user.email}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{
                        backgroundColor: '#EFF6FF',
                        color: '#0050A4',
                        padding: '3px 8px',
                        borderRadius: '10px',
                        fontSize: '11px',
                        fontWeight: 700
                      }}>
                        {user.role}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {user.branch || 'MAIN'}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {user.mfaEnrolled ? (
                        <span style={{ color: '#16A34A', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
                          <CheckCircle2 size={14} /> Enrolled
                        </span>
                      ) : (
                        <span style={{ color: '#ED7D16', fontWeight: 600 }}>Pending Setup</span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {user.accountNonLocked ? (
                        <span style={{ color: '#16A34A', fontWeight: 600 }}>Normal</span>
                      ) : (
                        <span style={{ color: '#DC2626', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Lock size={14} /> Locked
                        </span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '8px' }}>
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedUser(user);
                            setTargetRole(String(user.role));
                          }}
                          style={{
                            padding: '5px 10px',
                            backgroundColor: '#F1F5F9',
                            border: '1px solid #CBD5E1',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: 600,
                            cursor: 'pointer'
                          }}
                        >
                          Change Role
                        </button>

                        <button
                          type="button"
                          onClick={() => handleResetMfa(user)}
                          style={{
                            padding: '5px 10px',
                            backgroundColor: '#FFF7ED',
                            border: '1px solid #FED7AA',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: 600,
                            color: '#C2410C',
                            cursor: 'pointer'
                          }}
                        >
                          Reset 2FA
                        </button>

                        {!user.accountNonLocked && (
                          <button
                            type="button"
                            onClick={() => handleUnlockAccount(user)}
                            style={{
                              padding: '5px 10px',
                              backgroundColor: '#DCFCE7',
                              border: '1px solid #BBF7D0',
                              borderRadius: '4px',
                              fontSize: '11px',
                              fontWeight: 700,
                              color: '#166534',
                              cursor: 'pointer'
                            }}
                          >
                            Unlock
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Provision Staff Modal */}
      {showAddModal && (
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
              <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#0A2E65', margin: 0 }}>
                Provision New Bank Officer
              </h3>
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748B' }}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateStaff} style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                  Full Officer Name
                </label>
                <input
                  type="text"
                  value={newStaff.name}
                  onChange={(e) => setNewStaff({ ...newStaff, name: e.target.value })}
                  placeholder="e.g. Priya Sharma"
                  required
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                  Bank Email Address
                </label>
                <input
                  type="email"
                  value={newStaff.email}
                  onChange={(e) => setNewStaff({ ...newStaff, email: e.target.value })}
                  placeholder="priya.sharma@bankofindia.co.in"
                  required
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                  Temporary Password
                </label>
                <input
                  type="password"
                  value={newStaff.password}
                  onChange={(e) => setNewStaff({ ...newStaff, password: e.target.value })}
                  placeholder="Min 8 chars, 1 uppercase, 1 digit"
                  required
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                    Assigned Role
                  </label>
                  <select
                    value={newStaff.role}
                    onChange={(e) => setNewStaff({ ...newStaff, role: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '13px', boxSizing: 'border-box' }}
                  >
                    <option value="ROLE_LOAN_OFFICER">Loan Officer</option>
                    <option value="ROLE_COLLECTIONS_AGENT">Collections Agent</option>
                    <option value="ROLE_COLLECTIONS_MANAGER">Collections Manager</option>
                    <option value="ROLE_ADMIN">System Administrator</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                    Branch Jurisdiction
                  </label>
                  <select
                    value={newStaff.branch}
                    onChange={(e) => setNewStaff({ ...newStaff, branch: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '13px', boxSizing: 'border-box' }}
                  >
                    <option value="MAIN">MAIN</option>
                    <option value="SOUTH">SOUTH</option>
                    <option value="NORTH">NORTH</option>
                    <option value="EAST">EAST</option>
                    <option value="WEST">WEST</option>
                    <option value="ALL">ALL (Multi-branch)</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                <button
                  type="submit"
                  disabled={saving}
                  style={{
                    flex: 1,
                    padding: '12px',
                    backgroundColor: '#0050A4',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    fontWeight: 700,
                    fontSize: '14px',
                    cursor: saving ? 'not-allowed' : 'pointer'
                  }}
                >
                  {saving ? 'Provisioning...' : 'Confirm Provisioning'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Update Role Modal */}
      {selectedUser && (
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
            maxWidth: '440px',
            width: '100%',
            padding: '24px',
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.25)'
          }}>
            <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#0A2E65', margin: '0 0 8px' }}>
              Modify RBAC Role: {selectedUser.name}
            </h3>
            <p style={{ fontSize: '13px', color: '#64748B', marginBottom: '16px' }}>
              Current: <strong>{selectedUser.role}</strong>
            </p>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#0A2E65', marginBottom: '6px' }}>
                New System Role
              </label>
              <select
                value={targetRole}
                onChange={(e) => setTargetRole(e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid #CBD5E1', fontSize: '14px', boxSizing: 'border-box' }}
              >
                <option value="ROLE_LOAN_OFFICER">Loan Officer</option>
                <option value="ROLE_COLLECTIONS_AGENT">Collections Agent</option>
                <option value="ROLE_COLLECTIONS_MANAGER">Collections Manager</option>
                <option value="ROLE_ADMIN">System Administrator</option>
                <option value="ROLE_CUSTOMER">Customer</option>
              </select>
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                type="button"
                onClick={handleUpdateRole}
                disabled={saving}
                style={{
                  flex: 1,
                  padding: '10px',
                  backgroundColor: '#0050A4',
                  color: '#FFFFFF',
                  borderRadius: '6px',
                  border: 'none',
                  fontWeight: 700,
                  fontSize: '13px',
                  cursor: 'pointer'
                }}
              >
                {saving ? 'Updating...' : 'Save Role'}
              </button>
              <button
                type="button"
                onClick={() => setSelectedUser(null)}
                style={{
                  padding: '10px 16px',
                  backgroundColor: '#F1F5F9',
                  border: '1px solid #CBD5E1',
                  borderRadius: '6px',
                  fontWeight: 600,
                  fontSize: '13px',
                  cursor: 'pointer'
                }}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminStaffPage;
