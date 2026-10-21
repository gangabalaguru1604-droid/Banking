import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { MainLayout } from '../layouts/MainLayout';
import { AuthLayout } from '../layouts/AuthLayout';
import { ProtectedRoute } from '../components/ProtectedRoute';

// Pages
import HomePage from '../pages/HomePage';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import ForgotPasswordPage from '../pages/ForgotPasswordPage';
import ResetPasswordPage from '../pages/ResetPasswordPage';
import MfaVerifyPage from '../pages/MfaVerifyPage';
import MfaEnrollPage from '../pages/MfaEnrollPage';
import DashboardRouter from '../pages/DashboardRouter';
import ApplyLoanPage from '../pages/ApplyLoanPage';
import RepaymentPage from '../pages/RepaymentPage';
import LoanSearchPage from '../pages/LoanSearchPage';
import CustomerProfilePage from '../pages/CustomerProfilePage';
import Loan360DetailsPage from '../pages/Loan360DetailsPage';
import AdminStaffPage from '../pages/AdminStaffPage';
import AdminAuditLogsPage from '../pages/AdminAuditLogsPage';
import AdminRemindersPage from '../pages/AdminRemindersPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Pages with Main Layout */}
      <Route element={<MainLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/jsp-home" element={<HomePage />} />

        {/* Authenticated Dashboard */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardRouter />
            </ProtectedRoute>
          }
        />

        {/* Customer Operations */}
        <Route
          path="/applyLoan"
          element={
            <ProtectedRoute allowedRoles={['ROLE_CUSTOMER', 'ROLE_LOAN_OFFICER', 'ROLE_ADMIN']}>
              <ApplyLoanPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/pay"
          element={
            <ProtectedRoute allowedRoles={['ROLE_CUSTOMER']}>
              <RepaymentPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/payPage"
          element={
            <ProtectedRoute allowedRoles={['ROLE_CUSTOMER']}>
              <RepaymentPage />
            </ProtectedRoute>
          }
        />

        {/* Staff & Officer Operations */}
        <Route
          path="/loan-search"
          element={
            <ProtectedRoute allowedRoles={['ROLE_LOAN_OFFICER', 'ROLE_COLLECTIONS_AGENT', 'ROLE_COLLECTIONS_MANAGER', 'ROLE_ADMIN']}>
              <LoanSearchPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/customer-profile"
          element={
            <ProtectedRoute allowedRoles={['ROLE_LOAN_OFFICER', 'ROLE_COLLECTIONS_AGENT', 'ROLE_COLLECTIONS_MANAGER', 'ROLE_ADMIN']}>
              <CustomerProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/loan-details"
          element={
            <ProtectedRoute allowedRoles={['ROLE_LOAN_OFFICER', 'ROLE_COLLECTIONS_AGENT', 'ROLE_COLLECTIONS_MANAGER', 'ROLE_ADMIN']}>
              <Loan360DetailsPage />
            </ProtectedRoute>
          }
        />

        {/* Admin Management */}
        <Route
          path="/admin/staff"
          element={
            <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <AdminStaffPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/audit-logs"
          element={
            <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <AdminAuditLogsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/reminders"
          element={
            <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <AdminRemindersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/reminders/demo"
          element={
            <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <AdminRemindersPage />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* Auth Pages with Clean Auth Layout */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/mfa-verify" element={<MfaVerifyPage />} />
        <Route path="/mfa-enroll" element={<MfaEnrollPage />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRoutes;
