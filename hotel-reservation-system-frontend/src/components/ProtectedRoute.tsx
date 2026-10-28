import type { ReactNode } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { Role } from '../types/auth'

interface ProtectedRouteProps {
  role?: Role
  children: ReactNode
}

export default function ProtectedRoute({ role, children }: ProtectedRouteProps) {
  const { isAuthenticated, user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <p role="status" className="p-8 text-center text-gray-600">Cargando...</p>
  }

  if (!isAuthenticated) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }

  if (role && user?.role !== role) {
    return (
      <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-8 text-center">
        <h1 className="text-2xl font-semibold text-gray-900">403 - Acceso denegado</h1>
        <p role="alert" className="text-gray-600">No tienes permisos para ver esta página.</p>
        <Link to="/" className="text-orange-600 underline hover:text-orange-700">Volver al inicio</Link>
      </main>
    )
  }

  return <>{children}</>
}
