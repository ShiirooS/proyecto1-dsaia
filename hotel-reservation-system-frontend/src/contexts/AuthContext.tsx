import { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from 'react';
import { login as loginService, logout as logoutService, getToken } from '../services/authService';
import { getCurrentUser, UserDTO } from '../services/userService';
import { UNAUTHORIZED_EVENT } from '../services/authEvents';

export type Role = 'ADMIN' | 'USER';

interface AuthContextType {
  isAuthenticated: boolean;
  user: UserDTO | null;
  role: Role | null;
  isAdmin: boolean;
  loading: boolean;
  login: (username: string, password: string) => Promise<UserDTO>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function toRole(value: string | undefined): Role | null {
  return value === 'ADMIN' || value === 'USER' ? value : null;
}

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDTO | null>(null);
  const [loading, setLoading] = useState<boolean>(() => !!getToken());

  const logout = useCallback(() => {
    logoutService();
    setUser(null);
  }, []);

  useEffect(() => {
    if (!getToken()) return;
    let cancelled = false;
    getCurrentUser()
      .then((current) => {
        if (!cancelled) setUser(current);
      })
      .catch(() => {
        if (!cancelled) logout();
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [logout]);

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, logout);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, logout);
  }, [logout]);

  const login = useCallback(async (username: string, password: string) => {
    await loginService(username, password);
    try {
      const current = await getCurrentUser();
      setUser(current);
      return current;
    } catch (error) {
      logout();
      throw error;
    }
  }, [logout]);

  const value = useMemo<AuthContextType>(() => {
    const role = toRole(user?.role);
    return {
      isAuthenticated: user !== null,
      user,
      role,
      isAdmin: role === 'ADMIN',
      loading,
      login,
      logout,
    };
  }, [user, loading, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
