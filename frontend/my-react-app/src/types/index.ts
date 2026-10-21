export type AppRole =
  | 'ROLE_CUSTOMER'
  | 'ROLE_LOAN_OFFICER'
  | 'ROLE_COLLECTIONS_AGENT'
  | 'ROLE_COLLECTIONS_MANAGER'
  | 'ROLE_ADMIN';

export interface User {
  id: number;
  name: string;
  email: string;
  role: AppRole | string;
  branch?: string;
  customerId?: string;
  phoneNumber?: string;
  mfaEnrolled?: boolean;
}

export interface AuthState {
  authenticated: boolean;
  user: User | null;
  csrfToken?: string;
  csrfHeader?: string;
  csrfParameter?: string;
  loading: boolean;
}

export interface Loan {
  id: number;
  amount: number;
  interest: number;
  tenureMonths: number;
  emi: number;
  remaining: number;
  status: string;
  repaymentStatus?: string;
  dpd?: number;
  overdueAmount?: number;
  branch?: string;
  appliedDate?: string;
  cbsAccountNumber?: string;
  customerName?: string;
  customerEmail?: string;
  user?: User;
}

export interface StaffDashboardLoanDto {
  id: number;
  customerName: string;
  customerEmail: string;
  customerId: string;
  amount: number;
  interestRate: number;
  tenureMonths: number;
  emi: number;
  remaining: number;
  status: string;
  repaymentStatus: string;
  dpd: number;
  overdueAmount: number;
  branch: string;
  appliedDate: string;
  lastPaymentDate?: string;
  cbsAccountNumber?: string;
}

export interface CustomerActivityDto {
  title: string;
  description: string;
  timestamp: string;
  type: string;
  badgeColor?: string;
}

export interface CustomerProfileDto {
  userId: number;
  customerId: string;
  name: string;
  email: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  panNumber?: string;
  aadhaarNumber?: string;
  kycStatus?: string;
  creditScore?: number;
  employmentType?: string;
  monthlyIncome?: number;
  branch?: string;
  totalLoans?: number;
  activeLoans?: number;
  totalBorrowing?: number;
  currentOutstanding?: number;
  repaymentTrackRecord?: string;
  panMasked?: string;
  aadhaarMasked?: string;
}

export interface RepaymentInstallment {
  installmentNumber: number;
  dueDate: string;
  principalDue: number;
  interestDue: number;
  totalDue: number;
  principalPaid: number;
  interestPaid: number;
  totalPaid: number;
  status: string;
  dpd: number;
}

export interface PaymentTransaction {
  id?: number;
  transactionId: string;
  amount: number;
  paymentDate: string;
  paymentMode: string;
  referenceNumber: string;
  status: string;
}

export interface CbsAccountDTO {
  accountNumber: string;
  accountType: string;
  cifNumber: string;
  accountStatus: string;
  balance: number;
  lienAmount: number;
  ifscCode: string;
  branchCode: string;
  currency: string;
}

export interface CollateralDTO {
  collateralType: string;
  valuationAmount: number;
  ltvRatio: number;
  documentReference: string;
  verificationStatus: string;
  verifiedBy: string;
  valuationDate: string;
}

export interface LoanAccountDetailsDTO {
  id: number;
  loanId: string;
  customerId: string;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  sanctionAmount: number;
  interestRate: number;
  tenureMonths: number;
  emi: number;
  remainingAmount: number;
  totalPaid: number;
  status: string;
  repaymentStatus: string;
  dpd: number;
  dpdBucket: string;
  overdueAmount: number;
  branch: string;
  appliedDate: string;
  sanctionDate?: string;
  maturityDate?: string;
  cbsAccount?: CbsAccountDTO;
  collateral?: CollateralDTO;
  installments?: RepaymentInstallment[];
  transactions?: PaymentTransaction[];
}

export interface AuthAuditLog {
  id: number;
  timestamp: string;
  username: string;
  ipAddress: string;
  eventType: string;
  status: string;
  failureReason?: string;
  details?: string;
  adminUser?: string;
}

export interface StaffUser {
  id: number;
  name: string;
  email: string;
  role: AppRole | string;
  branch?: string;
  accountNonLocked: boolean;
  mfaEnrolled: boolean;
  failedAttemptCount: number;
  createdAt?: string;
}

export interface LoanReminderLog {
  id: number;
  customerName: string;
  customerEmail: string;
  loanId: number;
  dpd: number;
  channel: string;
  status: string;
  dispatchedAt: string;
  providerResponse?: string;
}
