import { motion } from 'framer-motion'
import { Loader2 } from 'lucide-react'
import { useMemo, useState } from 'react'
import { login, register } from '../services/api'
import { useAuth } from '../context/AuthContext'

const INPUT_STYLE =
  'w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-500/40'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateLogin({ email, password }) {
  const errors = {}
  if (!email || !EMAIL_PATTERN.test(email)) errors.email = 'Please enter a valid email.'
  if (!password || password.length < 6) errors.password = 'Password must be at least 6 characters.'
  return errors
}

function validateRegister({ fullName, email, password }) {
  const errors = validateLogin({ email, password })
  if (!fullName || fullName.trim().length < 2) errors.fullName = 'Full name must be at least 2 characters.'
  return errors
}

export default function AuthModal() {
  const { setSession } = useAuth()
  const [tab, setTab] = useState('login')
  const [loading, setLoading] = useState(false)
  const [serverError, setServerError] = useState('')
  const [values, setValues] = useState({
    fullName: '',
    email: '',
    password: '',
  })
  const [touched, setTouched] = useState({})

  const errors = useMemo(() => {
    return tab === 'login' ? validateLogin(values) : validateRegister(values)
  }, [tab, values])

  const canSubmit = Object.keys(errors).length === 0

  const onChange = (field) => (event) => {
    setValues((prev) => ({ ...prev, [field]: event.target.value }))
    setServerError('')
  }

  const onBlur = (field) => () => {
    setTouched((prev) => ({ ...prev, [field]: true }))
  }

  const onSubmit = async (event) => {
    event.preventDefault()
    setTouched({ fullName: true, email: true, password: true })
    if (!canSubmit) return

    setLoading(true)
    setServerError('')

    try {
      const result =
        tab === 'login'
          ? await login({ email: values.email, password: values.password })
          : await register(values)
      const token = result?.token || result?.data?.token
      const user = result?.user || result?.data?.user || { email: values.email, fullName: values.fullName || values.email.split('@')[0] }

      if (!token) {
        throw new Error('Token was not returned by auth service.')
      }
      setSession(token, user)
    } catch (error) {
      setServerError(error.message || 'Authentication failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 p-4">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(99,102,241,0.25),transparent_45%),radial-gradient(circle_at_80%_80%,rgba(6,182,212,0.12),transparent_40%)]" />
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative w-full max-w-md rounded-2xl border border-slate-800/80 bg-slate-900/80 p-6 shadow-glow backdrop-blur"
      >
        <h1 className="text-2xl font-semibold text-slate-100">Paper-Pilot AI</h1>
        <p className="mt-1 text-sm text-slate-400">Sign in to your workspace and continue your RAG sessions.</p>

        <div className="mt-5 grid grid-cols-2 rounded-xl bg-slate-950 p-1">
          {['login', 'register'].map((name) => (
            <button
              key={name}
              type="button"
              onClick={() => {
                setTab(name)
                setServerError('')
              }}
              className={`rounded-lg px-3 py-2 text-sm font-medium capitalize transition ${
                tab === name
                  ? 'bg-gradient-to-r from-violet-600 to-indigo-600 text-white'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {name}
            </button>
          ))}
        </div>

        <form onSubmit={onSubmit} className="mt-5 space-y-4">
          {tab === 'register' && (
            <div>
              <label htmlFor="fullName" className="mb-1 block text-sm text-slate-300">
                Full Name
              </label>
              <input
                id="fullName"
                value={values.fullName}
                onChange={onChange('fullName')}
                onBlur={onBlur('fullName')}
                className={`${INPUT_STYLE} ${touched.fullName && errors.fullName ? 'border-rose-500/70' : ''}`}
                placeholder="Ada Lovelace"
              />
              {touched.fullName && errors.fullName && <p className="mt-1 text-xs text-rose-400">{errors.fullName}</p>}
            </div>
          )}

          <div>
            <label htmlFor="email" className="mb-1 block text-sm text-slate-300">
              Email
            </label>
            <input
              id="email"
              type="email"
              value={values.email}
              onChange={onChange('email')}
              onBlur={onBlur('email')}
              className={`${INPUT_STYLE} ${touched.email && errors.email ? 'border-rose-500/70' : ''}`}
              placeholder="you@example.com"
            />
            {touched.email && errors.email && <p className="mt-1 text-xs text-rose-400">{errors.email}</p>}
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm text-slate-300">
              Password
            </label>
            <input
              id="password"
              type="password"
              value={values.password}
              onChange={onChange('password')}
              onBlur={onBlur('password')}
              className={`${INPUT_STYLE} ${touched.password && errors.password ? 'border-rose-500/70' : ''}`}
              placeholder="••••••••"
            />
            {touched.password && errors.password && <p className="mt-1 text-xs text-rose-400">{errors.password}</p>}
          </div>

          {serverError && <p className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-300">{serverError}</p>}

          <button
            type="submit"
            disabled={!canSubmit || loading}
            className="inline-flex w-full items-center justify-center rounded-lg bg-gradient-to-r from-violet-600 to-indigo-600 px-4 py-2.5 font-semibold text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {tab === 'login' ? 'Sign In' : 'Create Account'}
          </button>
        </form>
      </motion.div>
    </div>
  )
}
