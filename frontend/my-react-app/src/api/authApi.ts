import apiClient, { API_BASE_URL, setCsrfToken } from './client';

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
}

export interface ResetPasswordPayload {
  token: string;
  password: string;
  confirmPassword: string;
}

export const authApi = {
  async getStatus() {
    try {
      const res = await apiClient.get('/api/auth/status');
      if (res.data?.csrfToken) {
        setCsrfToken(res.data.csrfToken, res.data.csrfHeader);
      }
      return res.data;
    } catch {
      return { authenticated: false };
    }
  },

  async login(payload: LoginPayload) {
    const res = await apiClient.post('/api/auth/login', payload);
    return res.data;
  },

  async register(payload: RegisterPayload) {
    const res = await apiClient.post('/api/auth/register', payload);
    return res.data;
  },

  async getMfaVerifyData() {
    const res = await apiClient.get('/api/auth/mfa-verify-data');
    return res.data;
  },

  async verifyMfa(code: string) {
    const res = await apiClient.post('/api/auth/mfa-verify', { code });
    return res.data;
  },

  async getMfaEnrollData() {
    const res = await apiClient.get('/api/auth/mfa-enroll-data');
    return res.data;
  },

  async verifyMfaEnroll(code: string) {
    const res = await apiClient.post('/api/auth/mfa-enroll', { code });
    return res.data;
  },

  async forgotPassword(email: string) {
    const res = await apiClient.post('/api/auth/forgot-password', { email });
    return res.data;
  },

  async resetPassword(payload: ResetPasswordPayload) {
    const res = await apiClient.post('/api/auth/reset-password', payload);
    return res.data;
  },

  async keepAlive() {
    try {
      const res = await apiClient.post('/session/keep-alive');
      return res.status === 200;
    } catch {
      return false;
    }
  },

  async logout(csrfToken?: string, csrfParameter: string = '_csrf') {
    // Form submission ensures Spring Security session invalidation and cookie clearing
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `${API_BASE_URL}/logout`;

    if (csrfToken) {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = csrfParameter;
      input.value = csrfToken;
      form.appendChild(input);
    }

    document.body.appendChild(form);
    form.submit();
  }
};
