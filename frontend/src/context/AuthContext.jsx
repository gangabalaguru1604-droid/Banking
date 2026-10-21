import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Check localStorage for saved session or initialize default demo customer
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('ib_auth_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [sessionTimeRemaining, setSessionTimeRemaining] = useState(600); // 10 mins

  // Sync user changes to localStorage
  useEffect(() => {
    if (user) {
      localStorage.setItem('ib_auth_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('ib_auth_user');
    }
  }, [user]);

  // Session timeout countdown
  useEffect(() => {
    if (!user) return;
    const timer = setInterval(() => {
      setSessionTimeRemaining(prev => {
        if (prev <= 1) {
          logout();
          alert('Your session has expired due to 10 minutes of inactivity. Please log in again.');
          return 600;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [user]);

  const login = (userData) => {
    setUser(userData);
    setSessionTimeRemaining(600);
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('ib_auth_user');
  };

  const refreshSession = () => {
    setSessionTimeRemaining(600);
  };

  // Switch role helper for quick demo testing in the UI
  const switchRole = (role) => {
    if (!user) {
      setUser({
        id: 1,
        name: role === 'ROLE_ADMIN' ? 'Admin Officer' : role === 'ROLE_LOAN_OFFICER' ? 'Arun Kumar (Officer)' : role === 'ROLE_COLLECTIONS_AGENT' ? 'Pooja Verma (Collections)' : 'Rajesh Patil',
        email: role === 'ROLE_ADMIN' ? 'admin@bank.com' : 'user@bank.com',
        role: role
      });
    } else {
      setUser(prev => ({
        ...prev,
        role: role
      }));
    }
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, refreshSession, sessionTimeRemaining, switchRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
