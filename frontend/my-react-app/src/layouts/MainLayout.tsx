import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { Footer } from '../components/Footer';
import { SessionTimeoutModal } from '../components/SessionTimeoutModal';

export const MainLayout: React.FC = () => {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      backgroundColor: '#F7F9FC'
    }}>
      <Navbar />
      <SessionTimeoutModal />
      <main style={{ flex: 1, paddingBottom: '48px' }}>
        <Outlet />
      </main>
      <Footer />
    </div>
  );
};
