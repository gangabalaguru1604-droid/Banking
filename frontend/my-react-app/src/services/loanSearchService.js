/**
 * Loan Search Service for authorized bank staff.
 * Submits search queries via secure HTTP POST request with a JSON request body.
 * Sensitive customer search values (names, phone numbers, IDs, account numbers)
 * are NEVER passed in URL query strings or written to client-side console logs.
 */
import { API_BASE_URL, getBackendUrl } from '../utils/apiConfig';

/**
 * Searches loans across authorized branches using a secure POST request.
 *
 * @param {Object} params
 * @param {string} params.searchType - Category: 'ACCOUNT_NUMBER', 'CUSTOMER_ID', 'PHONE', 'NAME'
 * @param {string} params.query - The search query value
 * @param {number} [params.page=0] - Page index (0-based)
 * @param {number} [params.size=10] - Number of items per page
 * @param {string} [csrfToken] - Optional Spring Security CSRF token
 * @param {string} [csrfHeader='X-CSRF-TOKEN'] - Optional CSRF header name
 * @returns {Promise<Object>} Search response containing content, pagination, and messages
 */
export async function searchLoans({ searchType = 'ACCOUNT_NUMBER', query = '', page = 0, size = 10 }, csrfToken, csrfHeader = 'X-CSRF-TOKEN') {
  const endpoint = getBackendUrl('/api/loans/search');

  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  };

  if (csrfToken && csrfHeader) {
    headers[csrfHeader] = csrfToken;
  }

  try {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers,
      credentials: 'include',
      body: JSON.stringify({
        searchType,
        query: (query || '').trim(),
        page: Math.max(0, page),
        size: Math.max(1, size)
      })
    });

    if (response.status === 401 || response.status === 403) {
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        errorMessage: 'Unauthorized: Staff credentials required to access loan search.',
        unauthorized: true
      };
    }

    if (!response.ok) {
      let errorMsg = `Search request failed (HTTP ${response.status})`;
      try {
        const errorJson = await response.json();
        if (errorJson && (errorJson.message || errorJson.errorMessage)) {
          errorMsg = errorJson.message || errorJson.errorMessage;
        }
      } catch (e) {
        // Response was not JSON
      }
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        errorMessage: errorMsg
      };
    }

    const data = await response.json();
    return data;
  } catch (err) {
    // Note: Do NOT log the search query to console to prevent PII leakage in browser logs
    console.error('[LoanSearchService] Failed to execute loan search request.');
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      errorMessage: 'Unable to connect to loan search service. Please check your network connection.'
    };
  }
}

/**
 * Fetches the authorized branch configuration for the logged-in staff member.
 *
 * @returns {Promise<Object>} Object with authorizedBranch, officerName, role
 */
export async function fetchSearchConfig() {
  const endpoint = getBackendUrl('/api/loans/search/config');
  try {
    const response = await fetch(endpoint, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
      credentials: 'include'
    });
    if (response.ok) {
      return await response.json();
    }
  } catch (err) {
    // Non-blocking fallback
  }
  return { authorizedBranch: 'MAIN' };
}
