import apiClient from './client';

export interface CreateStaffPayload {
  name: string;
  email: string;
  password?: string;
  role: string;
  branch?: string;
}

export interface AuditLogParams {
  page?: number;
  size?: number;
  eventType?: string;
  username?: string;
  status?: string;
}

export interface SimulateWebhookPayload {
  loanId: number;
  dpd: number;
  eventType?: string;
  channel?: string;
}

export const adminApi = {
  async getStaffList() {
    const res = await apiClient.get('/api/admin/staff');
    return res.data;
  },

  async createStaff(payload: CreateStaffPayload) {
    const res = await apiClient.post('/api/admin/staff/create', payload);
    return res.data;
  },

  async updateStaffRole(userId: number, newRole: string) {
    const res = await apiClient.post('/api/admin/staff/update-role', { userId, newRole });
    return res.data;
  },

  async resetStaffMfa(userId: number) {
    const res = await apiClient.post('/api/admin/staff/reset-mfa', { userId });
    return res.data;
  },

  async unlockStaff(userId: number) {
    const res = await apiClient.post('/api/admin/staff/unlock', { userId });
    return res.data;
  },

  async getAuditLogs(params: AuditLogParams = {}) {
    const res = await apiClient.get('/api/admin/audit-logs', { params });
    return res.data;
  },

  async getRemindersDemoData() {
    const res = await apiClient.get('/api/admin/reminders/demo');
    return res.data;
  },

  async triggerReminderScheduler() {
    const res = await apiClient.post('/api/admin/reminders/trigger-scheduler');
    return res.data;
  },

  async simulateWebhook(payload: SimulateWebhookPayload) {
    const res = await apiClient.post('/api/admin/reminders/simulate-webhook', payload);
    return res.data;
  },

  async getPreviewHtml(loanId: number, dpd: number) {
    const res = await apiClient.get('/api/admin/reminders/preview-html', {
      params: { loanId, dpd }
    });
    return res.data;
  }
};
