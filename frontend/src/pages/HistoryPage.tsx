import { useEffect, useState } from 'react'
import { api, type Account, type Page, type Transaction } from '../api'

function inr(n: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(n)
}

export default function HistoryPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [accountId, setAccountId] = useState('')
  const [rows, setRows] = useState<Transaction[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    api<Account[]>('/api/v1/accounts')
      .then((list) => {
        setAccounts(list)
        if (list[0]) setAccountId(list[0].id)
      })
      .catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    if (!accountId) return
    api<Page<Transaction>>(`/api/v1/accounts/${accountId}/transactions?size=50`)
      .then((page) => setRows(page.content))
      .catch((e) => setError(e.message))
  }, [accountId])

  return (
    <div className="stack">
      <div>
        <h1>Transaction history</h1>
        <p className="muted">Ledger entries for the selected account.</p>
      </div>
      <div className="panel stack">
        <label>
          Account
          <select value={accountId} onChange={(e) => setAccountId(e.target.value)}>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.maskedAccountNumber} · {a.accountType}
              </option>
            ))}
          </select>
        </label>
        {error && <div className="error">{error}</div>}
        <table>
          <thead>
            <tr>
              <th>When</th>
              <th>Type</th>
              <th>Reference</th>
              <th>Amount</th>
              <th>Note</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((t) => (
              <tr key={t.id}>
                <td>{new Date(t.createdAt).toLocaleString()}</td>
                <td>{t.type}</td>
                <td>{t.reference}</td>
                <td>{inr(Number(t.amount))}</td>
                <td>{t.description ?? '—'}</td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  No transactions yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
