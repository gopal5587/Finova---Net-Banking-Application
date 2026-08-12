import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from './auth'

export default function Layout() {
  const { user, logout } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">Finova</div>
        <nav className="nav">
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Dashboard
          </NavLink>
          <NavLink to="/accounts" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Accounts
          </NavLink>
          <NavLink to="/transfer" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Transfer
          </NavLink>
          <NavLink to="/history" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            History
          </NavLink>
          {isAdmin && (
            <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : undefined)}>
              Admin
            </NavLink>
          )}
          <span className="muted">{user?.username}</span>
          <button className="btn secondary" type="button" onClick={logout}>
            Sign out
          </button>
        </nav>
      </header>
      <Outlet />
    </div>
  )
}
