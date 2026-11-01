import { Alert, Box, Button, CircularProgress } from '@mui/material'
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import RoleRoute from './auth/RoleRoute'
import { NotifyProvider } from './components/Notify'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import AdminHomePage from './pages/admin/AdminHomePage'
import TeacherHomePage from './pages/teacher/TeacherHomePage'
import StudentsPage from './pages/admin/StudentsPage'
import TeachersPage from './pages/admin/TeachersPage'
import CareersPage from './pages/admin/CareersPage'
import StudyPlansPage from './pages/admin/StudyPlansPage'
import CoursesPage from './pages/admin/CoursesPage'
import PeriodsPage from './pages/admin/PeriodsPage'
import SectionsPage from './pages/admin/SectionsPage'
import TeacherSectionsPage from './pages/teacher/TeacherSectionsPage'
import StudentHomePage from './pages/student/StudentHomePage'
import StudentPlanPage from './pages/student/StudentPlanPage'
import StudentOfferPage from './pages/student/StudentOfferPage'
import StudentEnrollPage from './pages/student/StudentEnrollPage'
import StudentHistoryPage from './pages/student/StudentHistoryPage'

const HOME = { ADMIN: '/admin/home', TEACHER: '/teacher/home', STUDENT: '/student/home' }

function AuthGate({ children }) {
  const { loading, error, logout } = useAuth()
  if (loading) {
    return (
        <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <CircularProgress />
        </Box>
    )
  }
  if (error) {
    return (
        <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 3 }}>
          <Alert severity="error" sx={{ maxWidth: 560 }} action={<Button color="inherit" onClick={logout}>Cerrar sesión</Button>}>
            No se pudo cargar tu perfil: {error}
            <br />
            Si el usuario existe en Keycloak, verifica que exista un estudiante o docente con el mismo correo.
          </Alert>
        </Box>
    )
  }
  return children
}

function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user ? HOME[user.role] : '/login'} replace />
}

function App() {
  return (
      <Router>
        <NotifyProvider>
          <AuthProvider>
            <AuthGate>
              <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/" element={<HomeRedirect />} />
                <Route element={<Layout />}>
                  <Route element={<RoleRoute roles={['ADMIN']} />}>
                    <Route path="/admin/home" element={<AdminHomePage />} />
                    <Route path="/admin/students" element={<StudentsPage />} />
                    <Route path="/admin/teachers" element={<TeachersPage />} />
                    <Route path="/admin/careers" element={<CareersPage />} />
                    <Route path="/admin/careers/:careerId/plans" element={<StudyPlansPage />} />
                    <Route path="/admin/plans/:planId/courses" element={<CoursesPage />} />
                    <Route path="/admin/periods" element={<PeriodsPage />} />
                    <Route path="/admin/periods/:periodId/sections" element={<SectionsPage />} />
                  </Route>
                  <Route element={<RoleRoute roles={['TEACHER']} />}>
                    <Route path="/teacher/home" element={<TeacherHomePage />} />
                    <Route path="/teacher/sections" element={<TeacherSectionsPage />} />
                  </Route>
                  <Route element={<RoleRoute roles={['STUDENT']} />}>
                    <Route path="/student/home" element={<StudentHomePage />} />
                    <Route path="/student/plan" element={<StudentPlanPage />} />
                    <Route path="/student/offer" element={<StudentOfferPage />} />
                    <Route path="/student/enroll" element={<StudentEnrollPage />} />
                    <Route path="/student/history" element={<StudentHistoryPage />} />
                  </Route>
                </Route>
                <Route path="*" element={<HomeRedirect />} />
              </Routes>
            </AuthGate>
          </AuthProvider>
        </NotifyProvider>
      </Router>
  )
}

export default App