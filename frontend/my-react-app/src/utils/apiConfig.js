/**
 * API and Route URL configuration for Smart Loan Management System.
 * Supports both standalone frontend deployments (e.g., Render Static Site with VITE_API_BASE_URL)
 * and unified monolith deployments (where frontend is served directly from Spring Boot).
 */

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');

/**
 * Returns a fully qualified or relative URL to the backend.
 * If VITE_API_BASE_URL is configured (e.g. https://indian-bank-loan-app.onrender.com),
 * it prepends it to backend routes (like /login, /register, /dashboard).
 * If VITE_API_BASE_URL is empty, it keeps the path relative (ideal for local dev with Vite proxy or unified deployment).
 */
export function getBackendUrl(path = '') {
  if (!path) return API_BASE_URL || '/';
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('#')) {
    return path;
  }
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  return API_BASE_URL ? `${API_BASE_URL}${cleanPath}` : cleanPath;
}
