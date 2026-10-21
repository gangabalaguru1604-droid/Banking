import apiClient from './client';

export interface ApplyLoanPayload {
  amount: number;
  tenureMonths?: number;
  interest?: number;
  monthlyIncome?: number;
  existingObligations?: number;
}

export interface PaymentPayload {
  loanId: number;
  amount: number;
  paymentMode: string;
  referenceNumber: string;
}

export interface RecordCollectionPayload {
  loanId: number;
  amount: number;
  paymentMode: string;
  referenceNumber: string;
  collectionRemarks?: string;
}

export interface SearchLoansParams {
  query?: string;
  branch?: string;
  status?: string;
  dpdBucket?: string;
  minAmount?: number;
  maxAmount?: number;
  sortBy?: string;
  sortDir?: string;
  page?: number;
  size?: number;
}

// Mongo-backed Loan documents serialize their key as `loanId`; older dashboard
// components consume `id`. Normalize at the API boundary so both sources work.
function normalizeLoan<T extends Record<string, any>>(loan: T): T & { id: number } {
  const id = loan.id ?? loan.loanId ?? loan.loanID ?? loan.loan_id;
  return { ...loan, id: id == null ? Number.NaN : Number(id) };
}

function normalizeLoanList(value: unknown) {
  return Array.isArray(value) ? value.map(normalizeLoan) : value;
}

export const loanApi = {
  async getDashboardData() {
    const res = await apiClient.get('/api/dashboard/data');
    return {
      ...res.data,
      loans: normalizeLoanList(res.data?.loans) ?? [],
      pendingLoans: normalizeLoanList(res.data?.pendingLoans) ?? [],
      delinquentLoans: normalizeLoanList(res.data?.delinquentLoans) ?? [],
      activeLoans: normalizeLoanList(res.data?.activeLoans) ?? []
    };
  },

  async getLoanTerms() {
    const res = await apiClient.get('/api/applyLoan/terms');
    return res.data;
  },

  async applyLoan(payload: ApplyLoanPayload) {
    const res = await apiClient.post('/api/applyLoan', payload);
    return res.data;
  },

  async getPaymentDetails(loanId: number) {
    const res = await apiClient.get('/api/pay/details', {
      params: { loanId }
    });
    return res.data?.loan
      ? { ...res.data, loan: normalizeLoan(res.data.loan) }
      : normalizeLoan(res.data);
  },

  async submitPayment(payload: PaymentPayload) {
    const res = await apiClient.post('/api/pay', payload);
    return res.data;
  },

  async approveLoan(loanId: number) {
    const res = await apiClient.post('/api/staff/approve-loan', { loanId });
    return res.data;
  },

  async rejectLoan(loanId: number, rejectionReason?: string) {
    const res = await apiClient.post('/api/staff/reject-loan', { loanId, rejectionReason });
    return res.data;
  },

  async recordCollection(payload: RecordCollectionPayload) {
    const res = await apiClient.post('/api/staff/record-collection', payload);
    return res.data;
  },

  async searchLoans(params: SearchLoansParams) {
    const res = await apiClient.get('/api/loan-search', { params });
    const data = res.data;
    if (Array.isArray(data)) return data.map(normalizeLoan);
    if (Array.isArray(data?.content)) return { ...data, content: data.content.map(normalizeLoan) };
    if (Array.isArray(data?.loans)) return { ...data, loans: data.loans.map(normalizeLoan) };
    return data;
  },

  async getCustomerProfile(customerId: string) {
    const res = await apiClient.get('/api/customer-profile', {
      params: { customerId }
    });
    return res.data;
  },

  async revealSensitiveField(customerId: string, fieldName: string) {
    const res = await apiClient.post('/api/customer-profile/reveal-field', {
      customerId,
      fieldName
    });
    return res.data;
  },

  async getLoanAccountDetails(loanId: number | string) {
    const res = await apiClient.get('/api/loan-details', {
      params: { loanId }
    });
    return res.data;
  },

  getPdfStatementUrl(loanId: number | string) {
    return `/loan-details/${loanId}/statement.pdf`;
  }
};
