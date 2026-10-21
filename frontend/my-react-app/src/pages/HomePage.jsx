import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import HeroSection from '../components/HeroSection';
import EmiCalculator from '../components/EmiCalculator';
import FeaturesSection from '../components/FeaturesSection';
import TrustBadges from '../components/TrustBadges';
import Footer from '../components/Footer';
import SessionTimeoutModal from '../components/SessionTimeoutModal';
import { fetchAuthStatus } from '../services/authService';

export default function HomePage() {
  const [authState, setAuthState] = useState({
    authenticated: false,
    user: null,
    csrfToken: '',
    csrfHeader: 'X-CSRF-TOKEN',
    csrfParameter: '_csrf'
  });

  useEffect(() => {
    let isMounted = true;

    async function loadAuth() {
      const data = await fetchAuthStatus();
      if (isMounted && data) {
        setAuthState({
          authenticated: !!data.authenticated,
          user: data.authenticated ? {
            name: data.name,
            email: data.email,
            role: data.role
          } : null,
          csrfToken: data.csrfToken || '',
          csrfHeader: data.csrfHeader || 'X-CSRF-TOKEN',
          csrfParameter: data.csrfParameter || '_csrf'
        });
      }
    }

    loadAuth();

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="home-page-layout">
      {/* Navbar with auth and role awareness */}
      <Navbar authState={authState} />

      {/* Main Hero section replicating Smart Loan Management index.jsp */}
      <main>
        <HeroSection authState={authState} />

        {/* Interactive EMI calculator matching project predefined terms */}
        <EmiCalculator authState={authState} />

        {/* Feature / Loan Products showcase */}
        <FeaturesSection authState={authState} />

        {/* Enterprise security pillars & trust badges */}
        <TrustBadges />
      </main>

      {/* Institutional Banking Footer */}
      <Footer />

      {/* 10-Minute Inactivity Warning Modal (only active if user is authenticated) */}
      <SessionTimeoutModal authState={authState} />
    </div>
  );
}
