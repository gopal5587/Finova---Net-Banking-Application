import { FormEvent, useEffect, useState } from 'react'
import { api, type Account } from '../api'

export default function TransferPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [fromAccountId, setFrom] = useState('')
  const [toAccountId, setTo] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  useEffect(() => {
    api<Account[]>('/api/v1/accounts')
      .then((list) => {
        setAccounts(list)
        if (list[0]) setFrom(list[0].id)
        if (list[1]) setTo(list[1].id)
      })
      .catch((e) => setError(e.message))
  }, [])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setOk('')
    try {
      const tx = await api<{ reference: string }>('/api/v1/transfers', {
        method: 'POST',
        body: JSON.stringify({
          fromAccountId,
          toAccountId,
          amount: Number(amount),
          description: description || null,
        }),
      })
      setOk(`Transfer settled as ${tx.reference}`)
      setAmount('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Transfer failed')
    }
  }

  return (
    <div className="stack">
      <div>
        <h1>Transfer</h1>
        <p className="muted">Move money between Finova accounts with ACID settlement.</p>
      </div>
      <form className="panel stack" style={{ maxWidth: 520 }} onSubmit={onSubmit}>
        <label>
          From
          <select value={fromAccountId} onChange={(e) => setFrom(e.target.value)} required>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.maskedAccountNumber} · {a.accountType} · ₹{a.balance}
              </option>
            ))}
          </select>
        </label>
        <label>
          To (account id)
          <input
            value={toAccountId}
            onChange={(e) => setTo(e.target.value)}
            placeholder="Destination account UUID"
            required
          />
        </label>
        {accounts.length > 1 && (
          <p className="muted">
            Tip: pick another of yours:{' '}
            {accounts
              .filter((a) => a.id !== fromAccountId)
              .map((a) => (
                <button
                  key={a.id}
                  type="button"
                  className="btn secondary"
                  style={{ marginRight: 8, marginTop: 4 }}
                  onClick={() => setTo(a.id)}
                >
                  {a.maskedAccountNumber}
                </button>
              ))}
          </p>
        )}
        <label>
          Amount (INR)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </label>
        <label>
          Note
          <input value={description} onChange={(e) => setDescription(e.target.value)} />
        </label>
        {error && <div className="error">{error}</div>}
        {ok && <div className="ok">{ok}</div>}
        <button className="btn" type="submit">
          Send transfer
        </button>
      </form>
    </div>
  )
}
