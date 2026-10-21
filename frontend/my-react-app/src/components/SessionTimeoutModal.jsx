import React, { useEffect, useState, useRef } from 'react';
import { sendKeepAlive, performLogout } from '../services/authService';
import { getBackendUrl } from '../utils/apiConfig';

export default function SessionTimeoutModal({ authState }) {
  const { authenticated, csrfToken, csrfHeader, csrfParameter } = authState;

  const TOTAL_TIMEOUT_SEC = 600; // 10 minutes
  const WARNING_TIME_SEC = 60;   // 60 seconds countdown
  const INACTIVITY_LIMIT_SEC = TOTAL_TIMEOUT_SEC - WARNING_TIME_SEC; // 540 seconds

  const [modalVisible, setModalVisible] = useState(false);
  const [countdown, setCountdown] = useState(WARNING_TIME_SEC);

  const inactivityTimerRef = useRef(0);
  const modalVisibleRef = useRef(false);

  useEffect(() => {
    if (!authenticated) {
      return;
    }

    let lastThrottledTime = 0;
    const resetInactivity = () => {
      const now = Date.now();
      // Throttle updates: avoid triggering on every single mouse pixel movement
      if (now - lastThrottledTime > 5000 && !modalVisibleRef.current) {
        lastThrottledTime = now;
        inactivityTimerRef.current = 0;
      }
    };

    const listenerOptions = { passive: true };
    window.addEventListener('mousemove', resetInactivity, listenerOptions);
    window.addEventListener('keydown', resetInactivity, listenerOptions);
    window.addEventListener('click', resetInactivity, listenerOptions);

    // Heartbeat ticker checks inactivity every second
    const interval = setInterval(() => {
      if (!modalVisibleRef.current) {
        inactivityTimerRef.current += 1;
        if (inactivityTimerRef.current >= INACTIVITY_LIMIT_SEC) {
          modalVisibleRef.current = true;
          setModalVisible(true);
          setCountdown(WARNING_TIME_SEC);
        }
      }
    }, 1000);

    return () => {
      clearInterval(interval);
      window.removeEventListener('mousemove', resetInactivity);
      window.removeEventListener('keydown', resetInactivity);
      window.removeEventListener('click', resetInactivity);
    };
  }, [authenticated]);

  // Countdown timer when modal is active
  useEffect(() => {
    if (!modalVisible) return;

    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          // Automatic logout on timeout
          window.location.href = getBackendUrl('/login?expired=true');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [modalVisible]);

  const handleStayLoggedIn = async () => {
    const success = await sendKeepAlive(csrfToken, csrfHeader);
    if (success) {
      inactivityTimerRef.current = 0;
      modalVisibleRef.current = false;
      setModalVisible(false);
    } else {
      // If session expired on server already
      window.location.href = getBackendUrl('/login?expired=true');
    }
  };

  const handleLogoutNow = () => {
    performLogout(csrfToken, csrfParameter);
  };

  if (!authenticated || !modalVisible) {
    return null;
  }

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div className="modal-card">
        <h3 id="modal-title" style={{ marginTop: 0, color: 'var(--dark-navy)' }}>
          Session Inactivity Warning
        </h3>
        <p style={{ color: 'var(--text-muted)', fontSize: '14px', margin: '8px 0 16px' }}>
          Your session is about to expire due to inactivity.
        </p>

        <div className="countdown-number" aria-live="assertive">
          {countdown}
        </div>

        <p style={{ color: 'var(--text-muted)', fontSize: '13px', margin: '0 0 24px' }}>
          Seconds remaining before automatic logout
        </p>

        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <button 
            type="button" 
            className="btn" 
            style={{ width: '150px' }} 
            onClick={handleStayLoggedIn}
          >
            Stay Logged In
          </button>
          <button 
            type="button" 
            className="btn btn-secondary" 
            style={{ width: '150px' }} 
            onClick={handleLogoutNow}
          >
            Log Out Now
          </button>
        </div>
      </div>
    </div>
  );
}
