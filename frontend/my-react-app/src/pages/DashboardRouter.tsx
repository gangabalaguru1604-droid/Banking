import React from 'react';
import { useAuth } from '../context/AuthContext';
import CustomerDashboard from './CustomerDashboard';
import StaffDashboard from './StaffDashboard';

export const DashboardRouter: React.FC = () => {
  const { authState } = useAuth();
  const role = authState.user?.role;

  if (role === 'ROLE_CUSTOMER') {
    return <CustomerDashboard />;
  }

  return <StaffDashboard />;
};

export default DashboardRouter;
