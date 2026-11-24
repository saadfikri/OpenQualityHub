import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import type { AuthResponse } from '@/types/api';

interface AuthState {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  displayName: string;
  organizationId: string;
  permissions: string[];
}

interface AuthContextType {
  auth: AuthState | null;
  login: (response: AuthResponse) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

const STORAGE_KEY = 'oqh_auth';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch {
        return null;
      }
    }
    return null;
  });

  const login = useCallback((response: AuthResponse) => {
    const state: AuthState = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: response.userId,
      email: response.email,
      displayName: response.displayName,
      organizationId: response.organizationId,
      permissions: response.permissions,
    };
    setAuth(state);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }, []);

  const logout = useCallback(() => {
    setAuth(null);
    localStorage.removeItem(STORAGE_KEY);
  }, []);

  return (
    <AuthContext.Provider value={{ auth, login, logout, isAuthenticated: !!auth }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
