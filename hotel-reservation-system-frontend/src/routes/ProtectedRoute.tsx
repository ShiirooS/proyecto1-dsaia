import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth, Role } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  redirectPath?: string;
  requiredRole?: Role;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  redirectPath = '/admin/login',
  requiredRole,
}) => {
  const { isAuthenticated, role, loading } = useAuth();

  if (loading) {
    return <div role="status" className="p-8 text-center text-gray-500">Cargando...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to={redirectPath} replace />;
  }

  if (requiredRole && role !== requiredRole) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};
