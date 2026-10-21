/**
 * Authentication and Session Service for Smart Loan Management System.
 * Connects to Spring Boot backend preserving Spring Security and session handling.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export async function fetchAuthStatus() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/status`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      },
      credentials: 'include' // Transmit JSESSIONID cookie
    });

    if (!response.ok) {
      return { authenticated: false };
    }

    const data = await response.json();
    return data;
  } catch (err) {
    // Graceful fallback for disconnected dev or network error
    console.debug('[AuthService] Could not connect to /api/auth/status:', err.message);
    return { authenticated: false };
  }
}

export async function sendKeepAlive(csrfToken, csrfHeader) {
  try {
    const headers = {
      'Accept': 'application/json'
    };
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    const response = await fetch(`${API_BASE_URL}/session/keep-alive`, {
      method: 'POST',
      headers,
      credentials: 'include'
    });

    return response.ok;
  } catch (err) {
    console.error('[AuthService] Keep-alive error:', err);
    return false;
  }
}

export function performLogout(csrfToken, csrfParameter = '_csrf') {
  // Use standard form POST to /logout so Spring Security logout handlers,
  // cookie clearing (JSESSIONID), and session invalidation execute natively.
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
