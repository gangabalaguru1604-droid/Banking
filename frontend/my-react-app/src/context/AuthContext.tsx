import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { AuthState, User } from '../types';
import { authApi } from '../api/authApi';

interface AuthContextType {
  authState: AuthState;
  refreshAuth: () => Promise<void>;
  logout: () => Promise<void>;
  keepAlive: () => Promise<boolean>;
  showTimeoutModal: boolean;
  secondsRemaining: number;
  stayLoggedIn: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const INACTIVITY_LIMIT_SEC = 600; // 10 minutes
const WARNING_THRESHOLD_SEC = 60; // Warning modal triggers at 60s remaining (540s of inactivity)

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>({
    authenticated: false,
    user: null,
    loading: true
  });

  const [showTimeoutModal, setShowTimeoutModal] = useState<boolean>(false);
  const [secondsRemaining, setSecondsRemaining] = useState<number>(WARNING_THRESHOLD_SEC);

  const lastActivityRef = useRef<number>(Date.now());
  const timerRef = useRef<number | null>(null);

  const refreshAuth = useCallback(async () => {
    try {
      const data = await authApi.getStatus();
      if (data && data.authenticated) {
        const user: User = {
          id: data.id || 0,
          name: data.name || '',
          email: data.email || '',
          role: data.role || '',
          branch: data.branch || 'MAIN',
          customerId: data.customerId || '',
          phoneNumber: data.phoneNumber || '',
          mfaEnrolled: data.mfaEnrolled ?? true
        };
        setAuthState({
          authenticated: true,
          user,
          csrfToken: data.csrfToken,
          csrfHeader: data.csrfHeader,
          csrfParameter: data.csrfParameter,
          loading: false
        });
      } else {
        setAuthState({
          authenticated: false,
          user: null,
          csrfToken: data?.csrfToken,
          csrfHeader: data?.csrfHeader,
          csrfParameter: data?.csrfParameter,
          loading: false
        });
      }
    } catch (err) {
      setAuthState({
        authenticated: false,
        user: null,
        loading: false
      });
    }
  }, []);

  const logout = useCallback(async () => {
    setShowTimeoutModal(false);
    await authApi.logout(authState.csrfToken, authState.csrfParameter);
  }, [authState.csrfToken, authState.csrfParameter]);

  const keepAlive = useCallback(async () => {
    const success = await authApi.keepAlive();
    if (success) {
      lastActivityRef.current = Date.now();
      setShowTimeoutModal(false);
      setSecondsRemaining(WARNING_THRESHOLD_SEC);
    }
    return success;
  }, []);

  const stayLoggedIn = useCallback(async () => {
    await keepAlive();
  }, [keepAlive]);

  // Track user activity
  useEffect(() => {
    const handleUserActivity = () => {
      // Only reset activity timestamp if warning modal is not active
      if (!showTimeoutModal) {
        lastActivityRef.current = Date.now();
      }
    };

    window.addEventListener('mousemove', handleUserActivity);
    window.addEventListener('keydown', handleUserActivity);
    window.addEventListener('click', handleUserActivity);
    window.addEventListener('scroll', handleUserActivity);

    return () => {
      window.removeEventListener('mousemove', handleUserActivity);
      window.removeEventListener('keydown', handleUserActivity);
      window.removeEventListener('click', handleUserActivity);
      window.removeEventListener('scroll', handleUserActivity);
    };
  }, [showTimeoutModal]);

  // Timer loop for inactivity check
  useEffect(() => {
    if (!authState.authenticated) {
      setShowTimeoutModal(false);
      return;
    }

    lastActivityRef.current = Date.now();

    const interval = window.setInterval(() => {
      const now = Date.now();
      const elapsedSec = Math.floor((now - lastActivityRef.current) / 1000);
      const remainingSec = INACTIVITY_LIMIT_SEC - elapsedSec;

      if (remainingSec <= 0) {
        // Inactivity limit reached -> logout
        logout();
      } else if (remainingSec <= WARNING_THRESHOLD_SEC) {
        setShowTimeoutModal(true);
        setSecondsRemaining(remainingSec);
      } else {
        if (showTimeoutModal) {
          setShowTimeoutModal(false);
        }
      }
    }, 1000);

    timerRef.current = interval;

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, [authState.authenticated, logout, showTimeoutModal]);

  // Initial auth status fetch
  useEffect(() => {
    refreshAuth();
  }, [refreshAuth]);

  return (
    <AuthContext.Provider
      value={{
        authState,
        refreshAuth,
        logout,
        keepAlive,
        showTimeoutModal,
        secondsRemaining,
        stayLoggedIn
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
