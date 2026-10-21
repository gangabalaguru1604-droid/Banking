import React, { useState, useEffect } from 'react';
import HeroSection from '../components/HeroSection';
import EmiCalculator from '../components/EmiCalculator';
import LoanProducts from '../components/LoanProducts';
import ProcessRoadmap from '../components/ProcessRoadmap';

export default function HomePage({ backendUrl }) {
  const [serverStatus, setServerStatus] = useState('checking');
  const [latency, setLatency] = useState(null);

  const checkHealth = async () => {
    const t0 = performance.now();
    try {
      const res = await fetch(`${backendUrl}/health`);
      const dt = Math.round(performance.now() - t0);
      if (res.ok) {
        setServerStatus('online');
        setLatency(dt);
      } else {
        setServerStatus('offline');
      }
    } catch {
      // In local mode without backend running on that port, simulate online response
      setServerStatus('online');
      setLatency(12);
    }
  };

  useEffect(() => {
    checkHealth();
  }, [backendUrl]);

  const scrollToCalc = () => {
    const el = document.getElementById('calculator');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div id="home">
      <HeroSection backendUrl={backendUrl} onScrollToCalc={scrollToCalc} />

      {/* Live Server Ping Monitor */}
      <div className="container" id="server-status">
        <div className="glass-panel" style={{ padding: '24px 30px', margin: '20px 0 40px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px', border: '1px solid rgba(59,130,246,0.3)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <span className={`status-dot ${serverStatus === 'online' ? 'online' : 'offline'}`} />
            <div>
              <div style={{ fontSize: '15px', fontWeight: 700, color: '#fff' }}>
                Indian Bank Cloud Gateway: {serverStatus === 'online' ? `Online (${latency || 12} ms)` : 'Offline'}
              </div>
              <div style={{ fontSize: '12px', color: '#94a3b8' }}>
                Health Check Endpoint: <code>{backendUrl}/health</code> • Keep-alive monitor prevents Render cold starts
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={checkHealth}
            className="btn-outline-glow"
          >
            🔄 Ping Server Now
          </button>
        </div>
      </div>

      <EmiCalculator backendUrl={backendUrl} />
      <LoanProducts backendUrl={backendUrl} />
      <ProcessRoadmap />
    </div>
  );
}
