import { useEffect, useState } from 'react'
import { api, type Page } from '../api'

type AdminAccount = {
  id: string
  maskedAccountNumber: string
  ownerUsername: string
  accountType: string
  balance: number
  status: string
}

type FraudFlag = {
  id: string
  accountId: string
  reason: string
  severity: string
  details: string
  resolved: boolean
  createdAt: string
}

export default function AdminPage() {
  const [accounts, setAccounts] = useState<AdminAccount[]>([])
  const [flags, setFlags] = useState<FraudFlag[]>([])
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  async function load() {
    const [a, f] = await Promise.all([
      api<Page<AdminAccount>>('/api/v1/admin/accounts?size=50'),
      api<Page<FraudFlag>>('/api/v1/admin/fraud-flags?resolved=false&size=50'),
    ])
    setAccounts(a.content)
    setFlags(f.content)
  }

  useEffect(() => {
    load().catch((e) => setError(e.message))
  }, [])

  async function freeze(id: string) {
    setError('')
    setOk('')
    try {
      await api(`/api/v1/admin/accounts/${id}/freeze`, { method: 'POST' })
      setOk('Account frozen')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed')
    }
  }

  async function unfreeze(id: string) {
    setError('')
    setOk('')
    try {
      await api(`/api/v1/admin/accounts/${id}/unfreeze`, { method: 'POST' })
      setOk('Account unfrozen')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed')
    }
  }

  async function resolveFlag(id: string) {
    setError('')
    setOk('')
    try {
      await api(`/api/v1/admin/fraud-flags/${id}/resolve`, { method: 'POST' })
      setOk('Flag resolved')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed')
    }
  }

  return (
    <div className="stack">
      <div>
        <h1>Admin oversight</h1>
        <p className="muted">Freeze accounts and review fraud flags.</p>
      </div>
      {error && <div className="error">{error}</div>}
      {ok && <div className="ok">{ok}</div>}
      <div className="panel stack">
        <h2 style={{ margin: 0 }}>Open fraud flags</h2>
        <table>
          <thead>
            <tr>
              <th>Reason</th>
              <th>Severity</th>
              <th>Details</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {flags.map((f) => (
              <tr key={f.id}>
                <td>{f.reason}</td>
                <td>{f.severity}</td>
                <td>{f.details}</td>
                <td>
                  <button className="btn secondary" type="button" onClick={() => resolveFlag(f.id)}>
                    Resolve
                  </button>
                </td>
              </tr>
            ))}
            {flags.length === 0 && (
              <tr>
                <td colSpan={4} className="muted">
                  No open flags.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="panel stack">
        <h2 style={{ margin: 0 }}>All accounts</h2>
        <table>
          <thead>
            <tr>
              <th>Owner</th>
              <th>Number</th>
              <th>Status</th>
              <th>Balance</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((a) => (
              <tr key={a.id}>
                <td>{a.ownerUsername}</td>
                <td>{a.maskedAccountNumber}</td>
                <td>{a.status}</td>
                <td>₹{a.balance}</td>
                <td>
                  {a.status === 'FROZEN' ? (
                    <button className="btn secondary" type="button" onClick={() => unfreeze(a.id)}>
                      Unfreeze
                    </button>
                  ) : (
                    <button className="btn danger" type="button" onClick={() => freeze(a.id)}>
                      Freeze
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
