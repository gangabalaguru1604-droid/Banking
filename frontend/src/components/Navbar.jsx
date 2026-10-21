import React from 'react';

export default function Navbar({ backendUrl, backendStatus, pingLatency, onPingServer, autoPing, setAutoPing }) {
  return (
    <header className="navbar-wrapper">
      <div className="container navbar-content">
        {/* Brand */}
        <a href="#home" className="brand-logo" id="nav-brand-logo">
          <span className="brand-icon">🏦</span>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span className="brand-title">Indian Bank</span>
              <span className="brand-tag">Lending 2.0</span>
            </div>
          </div>
        </a>

        {/* Navigation Links */}
        <nav>
          <ul className="nav-links">
            <li><a href="#loans" className="nav-link" id="nav-link-loans">Loan Products</a></li>
            <li><a href="#calculator" className="nav-link" id="nav-link-calc">EMI Calculator</a></li>
            <li><a href="#process" className="nav-link" id="nav-link-process">How It Works</a></li>
            <li><a href="#server-status" className="nav-link" id="nav-link-status">Server Monitor</a></li>
          </ul>
        </nav>

        {/* Actions & Health Badge */}
        <div className="nav-actions">
          {/* Live Backend Status Badge */}
          <div 
            className="backend-status-badge"
            onClick={onPingServer}
            title="Click to ping backend server and measure latency"
            id="nav-backend-badge"
          >
            <span className={`status-dot ${backendStatus === 'online' ? 'online' : backendStatus === 'checking' ? 'checking' : 'offline'}`} />
            <span>
              {backendStatus === 'online' && `Backend: ${pingLatency ? pingLatency + 'ms' : 'Online'}`}
              {backendStatus === 'checking' && 'Connecting...'}
              {backendStatus === 'offline' && 'Backend Offline'}
            </span>
          </div>

          {/* Auth Direct Links to Spring Boot Backend */}
          <a 
            href={`${backendUrl}/login`} 
            className="btn-secondary" 
            style={{ padding: '8px 16px', fontSize: '13px' }}
            id="btn-login"
          >
            Sign In
          </a>
          <a 
            href={`${backendUrl}/register`} 
            className="btn-primary" 
            style={{ padding: '8px 18px', fontSize: '13px' }}
            id="btn-register"
          >
            Apply Now
          </a>
        </div>
      </div>
    </header>
  );
}
