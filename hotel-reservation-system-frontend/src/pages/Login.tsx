import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthField from '../components/AuthField'
import { ApiError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { getSafeRedirect } from '../utils/redirect'

const REQUIRED_MESSAGES = {
  'username': 'Ingresa tu usuario o email',
  'password': 'Ingresa tu contraseña',
}

type FieldErrors = Partial<Record<keyof typeof REQUIRED_MESSAGES, string>>

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const redirect = getSafeRedirect(params.get('redirect'))
  const registerLink = params.get('redirect') ? `/register?redirect=${encodeURIComponent(redirect)}` : '/register'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextErrors: FieldErrors = {
      ...(!username.trim() && { username: REQUIRED_MESSAGES.username }),
      ...(!password && { 'password': REQUIRED_MESSAGES.password }),
    }
    setErrors(nextErrors)
    setServerError(null)
    if (Object.keys(nextErrors).length > 0) return

    setSubmitting(true)
    try {
      await login({ username: username.trim(), password })
      navigate(redirect, { replace: true })
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'No se pudo iniciar sesión. Inténtalo de nuevo.')
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-100 px-4">
      <div className="w-full max-w-md rounded-lg bg-white p-8 shadow">
        <Link to="/" className="font-bold text-orange-600">HOTEL MN</Link>
        <h1 className="mt-4 text-2xl font-semibold text-gray-900">Iniciar sesión</h1>
        <form onSubmit={handleSubmit} noValidate className="mt-6 space-y-4">
          <AuthField
            id="username"
            label="Usuario o email"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            error={errors.username}
          />
          <AuthField
            id="password"
            label="Contraseña"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errors.password}
          />
          {serverError && (
            <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-700">{serverError}</p>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-orange-600 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-orange-700 focus:outline-none focus:ring focus:ring-orange-400 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submitting ? 'Ingresando...' : 'Iniciar sesión'}
          </button>
        </form>
        <p className="mt-4 text-sm text-gray-600">
          ¿No tienes cuenta?{' '}
          <Link to={registerLink} className="text-orange-600 underline">Regístrate</Link>
        </p>
      </div>
    </main>
  )
}
