import { FormEvent, useEffect, useState } from 'react'
import { api, type Account } from '../api'

function inr(n: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(n)
}

export default function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [accountType, setAccountType] = useState('SAVINGS')
  const [initialDeposit, setInitialDeposit] = useState('0')
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  async function load() {
    setAccounts(await api<Account[]>('/api/v1/accounts'))
  }

  useEffect(() => {
    load().catch((e) => setError(e.message))
  }, [])

  async function onCreate(e: FormEvent) {
    e.preventDefault()
    setError('')
    setOk('')
    try {
      await api('/api/v1/accounts', {
        method: 'POST',
        body: JSON.stringify({
          accountType,
          initialDeposit: Number(initialDeposit),
        }),
      })
      setOk('Account opened.')
      setInitialDeposit('0')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed')
    }
  }

  return (
    <div className="stack">
      <div>
        <h1>Accounts</h1>
        <p className="muted">Open a savings or current account in INR.</p>
      </div>
      <div className="grid-2">
        <form className="panel stack" onSubmit={onCreate}>
          <h2 style={{ margin: 0 }}>Open account</h2>
          <label>
            Type
            <select value={accountType} onChange={(e) => setAccountType(e.target.value)}>
              <option value="SAVINGS">Savings</option>
              <option value="CURRENT">Current</option>
            </select>
          </label>
          <label>
            Initial deposit
            <input
              type="number"
              min="0"
              step="0.01"
              value={initialDeposit}
              onChange={(e) => setInitialDeposit(e.target.value)}
            />
          </label>
          {error && <div className="error">{error}</div>}
          {ok && <div className="ok">{ok}</div>}
          <button className="btn" type="submit">
            Open account
          </button>
        </form>
        <div className="panel stack">
          <h2 style={{ margin: 0 }}>Your accounts</h2>
          <table>
            <thead>
              <tr>
                <th>Number</th>
                <th>Type</th>
                <th>Balance</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((a) => (
                <tr key={a.id}>
                  <td>{a.maskedAccountNumber}</td>
                  <td>{a.accountType}</td>
                  <td>{inr(Number(a.balance))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
