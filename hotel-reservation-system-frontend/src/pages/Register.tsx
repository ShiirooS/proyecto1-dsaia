import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthField from '../components/AuthField'
import { ApiError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { getSafeRedirect } from '../utils/redirect'

interface RegisterForm {
  username: string
  email: string
  password: string
  fullName: string
  phone: string
}

type FormErrors = Partial<Record<keyof RegisterForm, string>>

const EMPTY_FORM: RegisterForm = { username: '', email: '', 'password': '', fullName: '', phone: '' }

const MESSAGES = {
  username: 'Entre 3 y 30 caracteres: letras, números, punto, guion o guion bajo',
  email: 'Ingresa un email válido',
  'password': 'Entre 8 y 72 caracteres, con al menos una letra y un número',
  fullName: 'El nombre completo es obligatorio',
}

function validate({ username, email, password, fullName }: RegisterForm): FormErrors {
  const weakPassword =
    password.length < 8 || password.length > 72 || !/[a-zA-Z]/.test(password) || !/\d/.test(password)
  return {
    ...(!/^[a-zA-Z0-9._-]{3,30}$/.test(username) && { username: MESSAGES.username }),
    ...(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) && { email: MESSAGES.email }),
    ...(weakPassword && { 'password': MESSAGES.password }),
    ...(!fullName.trim() && { fullName: MESSAGES.fullName }),
  }
}

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const redirect = getSafeRedirect(params.get('redirect'))
  const loginLink = params.get('redirect') ? `/login?redirect=${encodeURIComponent(redirect)}` : '/login'

  const [form, setForm] = useState<RegisterForm>(EMPTY_FORM)
  const [errors, setErrors] = useState<FormErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const bind = (field: keyof RegisterForm) => ({
    value: form[field],
    onChange: (e: ChangeEvent<HTMLInputElement>) => setForm({ ...form, [field]: e.target.value }),
    error: errors[field],
  })

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const nextErrors = validate(form)
    setErrors(nextErrors)
    setServerError(null)
    if (Object.keys(nextErrors).length > 0) return

    setSubmitting(true)
    try {
      const phone = form.phone.trim()
      await register({
        username: form.username,
        email: form.email.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        ...(phone && { phone }),
      })
      navigate(redirect, { replace: true })
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'No se pudo completar el registro. Inténtalo de nuevo.')
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-100 px-4 py-8">
      <div className="w-full max-w-md rounded-lg bg-white p-8 shadow">
        <Link to="/" className="font-bold text-orange-600">HOTEL MN</Link>
        <h1 className="mt-4 text-2xl font-semibold text-gray-900">Crear cuenta</h1>
        <form onSubmit={handleSubmit} noValidate className="mt-6 space-y-4">
          <AuthField id="fullName" label="Nombre completo" autoComplete="name" {...bind('fullName')} />
          <AuthField id="username" label="Usuario" autoComplete="username" {...bind('username')} />
          <AuthField id="email" label="Email" type="email" autoComplete="email" {...bind('email')} />
          <AuthField id="password" label="Contraseña" type="password" autoComplete="new-password" {...bind('password')} />
          <AuthField id="phone" label="Teléfono (opcional)" type="tel" autoComplete="tel" {...bind('phone')} />
          {serverError && (
            <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-700">{serverError}</p>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-orange-600 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-orange-700 focus:outline-none focus:ring focus:ring-orange-400 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submitting ? 'Registrando...' : 'Registrarse'}
          </button>
        </form>
        <p className="mt-4 text-sm text-gray-600">
          ¿Ya tienes cuenta?{' '}
          <Link to={loginLink} className="text-orange-600 underline">Inicia sesión</Link>
        </p>
      </div>
    </main>
  )
}
