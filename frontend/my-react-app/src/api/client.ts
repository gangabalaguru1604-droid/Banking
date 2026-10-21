import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');

let currentCsrfToken: string | null = null;
let currentCsrfHeader: string = 'X-CSRF-TOKEN';

export function setCsrfToken(token: string | null, header: string = 'X-CSRF-TOKEN') {
  currentCsrfToken = token;
  currentCsrfHeader = header || 'X-CSRF-TOKEN';
}

export function getCsrfToken(): string | null {
  return currentCsrfToken;
}

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  }
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // If we have a CSRF token and method is state-mutating, attach it
  const method = config.method?.toUpperCase();
  if (currentCsrfToken && method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
    if (config.headers) {
      config.headers[currentCsrfHeader] = currentCsrfToken;
    }
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export default apiClient;
