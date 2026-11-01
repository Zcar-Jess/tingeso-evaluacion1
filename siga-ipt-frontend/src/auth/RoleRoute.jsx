import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'

// Renders the child routes only when the logged user has one of the allowed roles.
export default function RoleRoute({ roles }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!roles.includes(user.role)) return <Navigate to="/" replace />
  return <Outlet />
}
