/**
 * Statement Service for downloading 360° Loan Account Statements as PDF Blobs.
 */
import { getBackendUrl } from '../utils/apiConfig';

/**
 * Downloads the official Bank of India Loan Statement PDF for a given loan ID.
 * Handles the response as a binary Blob and triggers client-side file save.
 * If backend returns an error (JSON or HTTP error), extracts and throws a meaningful error.
 *
 * @param {number|string} loanId - Loan ID to generate statement for
 * @param {string} [customFilename] - Optional override for filename
 * @returns {Promise<boolean>} True if download succeeded
 */
export async function downloadLoanStatementPdf(loanId, customFilename) {
  if (!loanId) {
    throw new Error('Valid Loan ID is required to download statement.');
  }

  const endpoint = getBackendUrl(`/loan-details/${loanId}/statement.pdf`);

  try {
    const response = await fetch(endpoint, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Accept': 'application/pdf, application/json'
      }
    });

    // Check for HTTP errors
    if (!response.ok) {
      let errorMessage = `Failed to generate statement (HTTP ${response.status} ${response.statusText})`;
      try {
        const errorJson = await response.json();
        if (errorJson && (errorJson.message || errorJson.error)) {
          errorMessage = errorJson.message || errorJson.error;
        }
      } catch (parseErr) {
        // Not JSON, use response text if available
        try {
          const text = await response.text();
          if (text && text.length < 200) errorMessage = text;
        } catch (tErr) {}
      }
      throw new Error(errorMessage);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      const errorJson = await response.json();
      throw new Error(errorJson.message || 'Backend returned an error instead of a PDF statement.');
    }

    // Process as binary Blob
    const blob = await response.blob();
    if (!blob || blob.size === 0) {
      throw new Error('Downloaded PDF statement is empty.');
    }

    // Extract filename from Content-Disposition header
    let filename = customFilename || `loan_statement_${loanId}.pdf`;
    const disposition = response.headers.get('content-disposition');
    if (disposition && disposition.includes('filename=')) {
      const match = disposition.match(/filename="?([^";]+)"?/);
      if (match && match[1]) {
        filename = match[1];
      }
    }

    // Trigger browser file download
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    // Clean up object URL after a brief delay
    setTimeout(() => {
      window.URL.revokeObjectURL(blobUrl);
    }, 1500);

    return true;
  } catch (error) {
    console.error('Error downloading loan statement PDF:', error);
    throw error;
  }
}
