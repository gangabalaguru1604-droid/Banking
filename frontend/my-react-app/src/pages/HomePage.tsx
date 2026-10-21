import React from 'react';
import HeroSection from '../components/HeroSection';
import EmiCalculator from '../components/EmiCalculator';
import FeaturesSection from '../components/FeaturesSection';
import TrustBadges from '../components/TrustBadges';
import { useAuth } from '../context/AuthContext';

export const HomePage: React.FC = () => {
  const { authState } = useAuth();

  return (
    <div className="home-page-layout">
      <HeroSection authState={authState} />
      <EmiCalculator authState={authState} />
      <FeaturesSection authState={authState} />
      <TrustBadges />
    </div>
  );
};

export default HomePage;
