import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Header from './components/Header';
import Footer from './components/Footer';

// Pages
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import MfaPage from './pages/MfaPage';
import MfaEnrollPage from './pages/MfaEnrollPage';
import DashboardPage from './pages/DashboardPage';
import ApplyLoanPage from './pages/ApplyLoanPage';
import PayPage from './pages/PayPage';
import LoanAccountDetailsPage from './pages/LoanAccountDetailsPage';
import LoanSearchPage from './pages/LoanSearchPage';
import CustomerProfilePage from './pages/CustomerProfilePage';
import AdminStaffPage from './pages/AdminStaffPage';
import AdminAuditsPage from './pages/AdminAuditsPage';
import AdminRemindersPage from './pages/AdminRemindersPage';

import './App.css';

export default function App() {
  const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8081';

  return (
    <AuthProvider>
      <BrowserRouter>
        {/* Ambient Glows */}
        <div className="ambient-glow">
          <div className="glow-orb-1"></div>
          <div className="glow-orb-2"></div>
          <div className="glow-orb-3"></div>
        </div>

        {/* Global Navigation Header (matches header.jsp) */}
        <Header />

        {/* Main Application Routes */}
        <main style={{ minHeight: '85vh', position: 'relative', zIndex: 1 }}>
          <Routes>
            <Route path="/" element={<HomePage backendUrl={backendUrl} />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route path="/mfa" element={<MfaPage />} />
            <Route path="/mfa-enroll" element={<MfaEnrollPage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/apply-loan" element={<ApplyLoanPage />} />
            <Route path="/applyLoan" element={<Navigate to="/apply-loan" replace />} />
            <Route path="/pay" element={<PayPage />} />
            <Route path="/payPage" element={<Navigate to="/pay" replace />} />
            <Route path="/loan-details/:id" element={<LoanAccountDetailsPage />} />
            <Route path="/loan-search" element={<LoanSearchPage />} />
            <Route path="/profile" element={<CustomerProfilePage />} />
            <Route path="/admin/staff" element={<AdminStaffPage />} />
            <Route path="/admin/audits" element={<AdminAuditsPage />} />
            <Route path="/admin/reminders" element={<AdminRemindersPage />} />
            {/* Fallback to Home */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>

        {/* Global Footer */}
        <Footer />
      </BrowserRouter>
    </AuthProvider>
  );
}
