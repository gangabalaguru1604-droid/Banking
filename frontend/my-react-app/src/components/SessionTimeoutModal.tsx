import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Clock, ShieldAlert } from 'lucide-react';

export const SessionTimeoutModal: React.FC = () => {
  const { showTimeoutModal, secondsRemaining, stayLoggedIn, logout } = useAuth();

  if (!showTimeoutModal) {
    return null;
  }

  const progressPercent = Math.max(0, Math.min(100, (secondsRemaining / 60) * 100));

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(10, 46, 101, 0.65)',
      backdropFilter: 'blur(4px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999,
      padding: '20px'
    }}>
      <div style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '12px',
        maxWidth: '440px',
        width: '100%',
        padding: '32px 28px',
        boxShadow: '0 20px 40px rgba(0, 0, 0, 0.25)',
        textAlign: 'center',
        position: 'relative',
        animation: 'slideUp 0.3s ease-out'
      }}>
        {/* Warning Icon Badge */}
        <div style={{
          width: '56px',
          height: '56px',
          borderRadius: '50%',
          backgroundColor: '#FFF7ED',
          border: '2px solid #FED7AA',
          color: '#ED7D16',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 16px'
        }}>
          <Clock size={28} />
        </div>

        <h3 style={{ fontSize: '20px', fontWeight: 800, color: '#0A2E65', marginBottom: '8px' }}>
          Session Expiring Soon
        </h3>
        
        <p style={{ fontSize: '14px', color: '#64748B', lineHeight: '1.5', marginBottom: '24px' }}>
          For security against unauthorized access, your banking session will automatically lock due to inactivity in:
        </p>

        {/* Circular Countdown Progress */}
        <div style={{
          position: 'relative',
          width: '100px',
          height: '100px',
          margin: '0 auto 24px'
        }}>
          <svg style={{ width: '100px', height: '100px', transform: 'rotate(-90deg)' }}>
            <circle
              cx="50"
              cy="50"
              r="42"
              stroke="#E2E8F0"
              strokeWidth="6"
              fill="transparent"
            />
            <circle
              cx="50"
              cy="50"
              r="42"
              stroke={secondsRemaining <= 15 ? '#DC2626' : '#ED7D16'}
              strokeWidth="6"
              fill="transparent"
              strokeDasharray={264}
              strokeDashoffset={264 - (264 * progressPercent) / 100}
              strokeLinecap="round"
              style={{ transition: 'stroke-dashoffset 1s linear, stroke 0.3s ease' }}
            />
          </svg>
          <div style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <span style={{
              fontSize: '24px',
              fontWeight: 800,
              color: secondsRemaining <= 15 ? '#DC2626' : '#0A2E65'
            }}>
              {secondsRemaining}s
            </span>
          </div>
        </div>

        <div style={{
          display: 'flex',
          gap: '12px',
          justifyContent: 'center'
        }}>
          <button
            type="button"
            onClick={() => stayLoggedIn()}
            style={{
              flex: 1,
              padding: '12px 20px',
              backgroundColor: '#0050A4',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '6px',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(0, 80, 164, 0.25)'
            }}
          >
            Stay Logged In
          </button>
          
          <button
            type="button"
            onClick={() => logout()}
            style={{
              padding: '12px 18px',
              backgroundColor: '#F8FAFC',
              color: '#64748B',
              border: '1px solid #CBD5E1',
              borderRadius: '6px',
              fontWeight: 600,
              fontSize: '14px',
              cursor: 'pointer'
            }}
          >
            Log Out Now
          </button>
        </div>
      </div>
    </div>
  );
};
