import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { AppRole } from '../types';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: (AppRole | string)[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const { authState } = useAuth();
  const location = useLocation();

  if (authState.loading) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '60vh',
        gap: '16px'
      }}>
        <div style={{
          width: '48px',
          height: '48px',
          border: '4px solid #E2E8F0',
          borderTopColor: '#0050A4',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite'
        }}></div>
        <p style={{ color: '#0A2E65', fontWeight: 600 }}>Securing Bank Session...</p>
        <style>{`
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  if (!authState.authenticated || !authState.user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const userRole = authState.user.role;
    if (!allowedRoles.includes(userRole)) {
      return (
        <div style={{ padding: '40px', textAlign: 'center', maxWidth: '600px', margin: '40px auto' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>🛡️</div>
          <h2 style={{ color: '#0A2E65', marginBottom: '8px' }}>Access Denied</h2>
          <p style={{ color: '#64748B', marginBottom: '24px' }}>
            Your account ({userRole}) does not have permission to access this resource.
          </p>
          <a href="/dashboard" className="btn" style={{ textDecoration: 'none' }}>
            Return to Dashboard
          </a>
        </div>
      );
    }
  }

  return <>{children}</>;
};
