import { FormEvent, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { api, type LoginResponse } from '../api'
import { useAuth } from '../auth'

type TokenPair = { accessToken: string; refreshToken: string; tokenType: string }

export default function LoginPage() {
  const { user, loginWithTokens } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [mfaToken, setMfaToken] = useState<string | null>(null)
  const [mfaCode, setMfaCode] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (mfaToken) {
        const tokens = await api<TokenPair>('/api/v1/auth/mfa/verify', {
          method: 'POST',
          body: JSON.stringify({ mfaToken, code: mfaCode }),
        })
        await loginWithTokens(tokens.accessToken)
        return
      }

      if (mode === 'register') {
        await api('/api/v1/auth/register', {
          method: 'POST',
          body: JSON.stringify({ username, password, email, fullName }),
        })
        setMode('login')
        setError('')
        setBusy(false)
        return
      }

      const res = await api<LoginResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      })
      if (res.mfaRequired && res.mfaToken) {
        setMfaToken(res.mfaToken)
      } else if (res.accessToken) {
        await loginWithTokens(res.accessToken)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="panel auth-card stack">
        <div>
          <div className="hero-mark">Finova</div>
          <h1>{mfaToken ? 'Enter your code' : mode === 'login' ? 'Sign in' : 'Create account'}</h1>
          <p className="muted">
            {mfaToken
              ? 'Open your authenticator app and enter the 6-digit code.'
              : 'Secure net banking for everyday INR accounts.'}
          </p>
        </div>
        <form className="stack" onSubmit={onSubmit}>
          {!mfaToken && mode === 'register' && (
            <>
              <label>
                Full name
                <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
              </label>
              <label>
                Email
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </label>
            </>
          )}
          {!mfaToken && (
            <>
              <label>
                Username
                <input value={username} onChange={(e) => setUsername(e.target.value)} required />
              </label>
              <label>
                Password
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
              </label>
            </>
          )}
          {mfaToken && (
            <label>
              Authentication code
              <input
                inputMode="numeric"
                pattern="\d{6}"
                maxLength={6}
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value)}
                required
              />
            </label>
          )}
          {error && <div className="error">{error}</div>}
          <button className="btn" disabled={busy} type="submit">
            {busy ? 'Please wait…' : mfaToken ? 'Verify' : mode === 'login' ? 'Sign in' : 'Register'}
          </button>
        </form>
        {!mfaToken && (
          <button
            type="button"
            className="btn secondary"
            onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
          >
            {mode === 'login' ? 'Need an account? Register' : 'Already registered? Sign in'}
          </button>
        )}
        {mfaToken && (
          <button type="button" className="btn secondary" onClick={() => setMfaToken(null)}>
            Back
          </button>
        )}
        <p className="muted">
          Demo admin: <code>admin</code> / <code>Admin@12345</code>
        </p>
        <Link to="/">Home</Link>
      </div>
    </div>
  )
}
