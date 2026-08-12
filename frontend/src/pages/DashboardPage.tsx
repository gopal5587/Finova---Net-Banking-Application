import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, type Account } from '../api'
import { useAuth } from '../auth'

function inr(n: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(n)
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [weather, setWeather] = useState<{ city: string; temperatureC: number; condition: string } | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([
      api<Account[]>('/api/v1/accounts'),
      api<{ city: string; temperatureC: number; condition: string }>('/api/v1/weather?city=Mumbai'),
    ])
      .then(([a, w]) => {
        setAccounts(a)
        setWeather(w)
      })
      .catch((e) => setError(e.message))
  }, [])

  const total = accounts.reduce((sum, a) => sum + Number(a.balance), 0)

  return (
    <div className="stack">
      <div>
        <h1>Welcome, {user?.fullName}</h1>
        <p className="muted">Your INR accounts at a glance.</p>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid-2">
        <div className="panel">
          <div className="muted">Total balance</div>
          <p className="balance">{inr(total)}</p>
          <p className="muted">{accounts.length} account(s)</p>
        </div>
        <div className="panel">
          <div className="muted">Local weather</div>
          <p className="balance" style={{ fontSize: '2rem' }}>
            {weather ? `${weather.temperatureC}°C` : '—'}
          </p>
          <p className="muted">
            {weather ? `${weather.city} · ${weather.condition}` : 'Loading…'}
          </p>
        </div>
      </div>
      <div className="panel stack">
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
          <h2 style={{ margin: 0 }}>Accounts</h2>
          <Link to="/accounts">Manage accounts</Link>
        </div>
        <table>
          <thead>
            <tr>
              <th>Number</th>
              <th>Type</th>
              <th>Status</th>
              <th>Balance</th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((a) => (
              <tr key={a.id}>
                <td>{a.maskedAccountNumber}</td>
                <td>{a.accountType}</td>
                <td>{a.status}</td>
                <td>{inr(Number(a.balance))}</td>
              </tr>
            ))}
            {accounts.length === 0 && (
              <tr>
                <td colSpan={4} className="muted">
                  No accounts yet. Open one from the Accounts page.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
