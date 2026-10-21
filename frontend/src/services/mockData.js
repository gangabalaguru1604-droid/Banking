// Realistic mock banking data corresponding to Spring Boot entities

export const initialLoans = [
  {
    id: 101,
    loanNumber: 'LN-2026-00101',
    customerName: 'Rajesh Patil',
    customerEmail: 'rajesh.patil@bank.com',
    customerPhone: '+91 98765 43210',
    amount: 250000,
    interestRate: 10.5,
    tenureMonths: 24,
    monthlyEmi: 11598,
    status: 'ACTIVE', // PENDING, APPROVED, REJECTED, ACTIVE, CLOSED, OVERDUE
    appliedDate: '2026-09-15',
    approvedDate: '2026-09-16',
    disbursedDate: '2026-09-16',
    purpose: 'Home Renovation & Solar Rooftop',
    dpdDays: 0,
    penalty: 0,
    totalPaid: 34794,
    remainingBalance: 215206,
    installmentsTotal: 24,
    installmentsPaid: 3,
    linkedAccount: {
      accountNumber: '•••• •••• 1234',
      bankName: 'Indian Bank Auto-Debit Account',
      ifscCode: 'IDIB0001001',
      accountType: 'SAVINGS',
      autoDebitEnabled: true,
      liveBalance: 145820.50
    }
  },
  {
    id: 102,
    loanNumber: 'LN-2026-00102',
    customerName: 'Suresh Patil',
    customerEmail: 'suresh.patil@example.com',
    customerPhone: '+91 91234 56789',
    amount: 150000,
    interestRate: 10.5,
    tenureMonths: 24,
    monthlyEmi: 6959,
    status: 'OVERDUE',
    appliedDate: '2026-08-10',
    approvedDate: '2026-08-11',
    disbursedDate: '2026-08-12',
    purpose: 'Commercial Vehicle Purchase',
    dpdDays: 2,
    penalty: 150.00,
    totalPaid: 20877,
    remainingBalance: 129123,
    installmentsTotal: 24,
    installmentsPaid: 3,
    linkedAccount: {
      accountNumber: '•••• •••• 5678',
      bankName: 'Indian Bank Auto-Debit Account',
      ifscCode: 'IDIB0001001',
      accountType: 'CURRENT',
      autoDebitEnabled: true,
      liveBalance: 3240.00
    }
  },
  {
    id: 103,
    loanNumber: 'LN-2026-00103',
    customerName: 'Meera Sharma',
    customerEmail: 'meera.sharma@example.com',
    customerPhone: '+91 98111 22334',
    amount: 500000,
    interestRate: 10.5,
    tenureMonths: 24,
    monthlyEmi: 23196,
    status: 'PENDING',
    appliedDate: '2026-10-20',
    approvedDate: null,
    disbursedDate: null,
    purpose: 'Higher Education Abroad',
    dpdDays: 0,
    penalty: 0,
    totalPaid: 0,
    remainingBalance: 500000,
    installmentsTotal: 24,
    installmentsPaid: 0,
    linkedAccount: {
      accountNumber: '•••• •••• 9012',
      bankName: 'Indian Bank Auto-Debit Account',
      ifscCode: 'IDIB0001001',
      accountType: 'SAVINGS',
      autoDebitEnabled: true,
      liveBalance: 85200.00
    }
  },
  {
    id: 104,
    loanNumber: 'LN-2026-00104',
    customerName: 'Vikram Joshi',
    customerEmail: 'vikram.j@example.com',
    customerPhone: '+91 97654 32109',
    amount: 800000,
    interestRate: 10.5,
    tenureMonths: 24,
    monthlyEmi: 37114,
    status: 'APPROVED',
    appliedDate: '2026-10-18',
    approvedDate: '2026-10-19',
    disbursedDate: null,
    purpose: 'MSME Working Capital Expansion',
    dpdDays: 0,
    penalty: 0,
    totalPaid: 0,
    remainingBalance: 800000,
    installmentsTotal: 24,
    installmentsPaid: 0,
    linkedAccount: {
      accountNumber: '•••• •••• 3456',
      bankName: 'Indian Bank Auto-Debit Account',
      ifscCode: 'IDIB0001001',
      accountType: 'CURRENT',
      autoDebitEnabled: true,
      liveBalance: 210000.00
    }
  }
];

export const initialStaff = [
  { id: 1, name: 'Arun Kumar', email: 'arun.officer@bank.com', role: 'ROLE_LOAN_OFFICER', status: 'ACTIVE', department: 'Credit Underwriting', createdDate: '2026-01-10' },
  { id: 2, name: 'Pooja Verma', email: 'pooja.agent@bank.com', role: 'ROLE_COLLECTIONS_AGENT', status: 'ACTIVE', department: 'Field Collections', createdDate: '2026-02-15' },
  { id: 3, name: 'Vikram Malhotra', email: 'vikram.manager@bank.com', role: 'ROLE_COLLECTIONS_MANAGER', status: 'ACTIVE', department: 'Recovery Oversight', createdDate: '2026-01-05' },
  { id: 4, name: 'Sanjay Deshmukh', email: 'admin@bank.com', role: 'ROLE_ADMIN', status: 'ACTIVE', department: 'Executive IT & Risk', createdDate: '2025-12-01' }
];

export const initialAudits = [
  { id: 'AUD-8821', timestamp: '2026-10-21 10:45:12', user: 'admin@bank.com', event: 'STAFF_ROLE_CHANGE', ip: '10.0.4.15', action: 'ASSIGN_ROLE_OFFICER', status: 'SUCCESS' },
  { id: 'AUD-8820', timestamp: '2026-10-21 10:32:05', user: 'pooja.agent@bank.com', event: 'COLLECTION_RECORDED', ip: '192.168.1.42', action: 'RECORD_DPD_REPAYMENT', status: 'SUCCESS' },
  { id: 'AUD-8819', timestamp: '2026-10-21 09:15:40', user: 'arun.officer@bank.com', event: 'LOAN_APPROVAL', ip: '10.0.2.88', action: 'APPROVE_LOAN_104', status: 'SUCCESS' },
  { id: 'AUD-8818', timestamp: '2026-10-21 09:00:01', user: 'SYSTEM_SCHEDULER', event: 'DPD_BATCH_TRIGGER', ip: '127.0.0.1', action: 'DISPATCH_REMINDERS_DPD1_2', status: 'SUCCESS' },
  { id: 'AUD-8817', timestamp: '2026-10-20 18:22:19', user: 'rajesh.patil@bank.com', event: 'MFA_AUTH', ip: '49.37.12.80', action: 'TOTP_VERIFICATION', status: 'SUCCESS' }
];

export const initialReminders = [
  { id: 'REM-101', loanId: 102, customerName: 'Suresh Patil', dpd: 2, overdueAmount: 6959.00, penalty: 150.00, totalDue: 7109.00, channel: 'SMS & Email', status: 'DELIVERED', dispatchTime: '2026-10-21 09:00:15' },
  { id: 'REM-102', loanId: 105, customerName: 'Kiran Rao', dpd: 1, overdueAmount: 11200.00, penalty: 0.00, totalDue: 11200.00, channel: 'SMS', status: 'QUEUED', dispatchTime: '2026-10-21 09:00:15' }
];
