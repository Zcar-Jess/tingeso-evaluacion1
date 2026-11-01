import { useState } from 'react'
import {
    AppBar, Avatar, Box, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText,
    Toolbar, Tooltip, Typography, useMediaQuery, useTheme,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import SchoolIcon from '@mui/icons-material/School'
import LogoutIcon from '@mui/icons-material/Logout'
import DashboardIcon from '@mui/icons-material/Dashboard'
import PeopleIcon from '@mui/icons-material/People'
import BadgeIcon from '@mui/icons-material/Badge'
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks'
import EventIcon from '@mui/icons-material/Event'
import GradeIcon from '@mui/icons-material/Grade'
import HomeIcon from '@mui/icons-material/Home'
import MenuBookIcon from '@mui/icons-material/MenuBook'
import ViewListIcon from '@mui/icons-material/ViewList'
import AppRegistrationIcon from '@mui/icons-material/AppRegistration'
import HistoryEduIcon from '@mui/icons-material/HistoryEdu'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const DRAWER_WIDTH = 252
const NAVY = '#0b1f3a'
const TEAL = '#2dd4bf'

const MENU = {
    ADMIN: [
        { title: 'Gestión académica', items: [
                { label: 'Resumen', path: '/admin/home', icon: <DashboardIcon /> },
                { label: 'Estudiantes', path: '/admin/students', icon: <PeopleIcon /> },
                { label: 'Docentes', path: '/admin/teachers', icon: <BadgeIcon /> },
                { label: 'Carreras y planes', path: '/admin/careers', icon: <LibraryBooksIcon /> },
            ] },
        { title: 'Períodos', items: [
                { label: 'Períodos y oferta', path: '/admin/periods', icon: <EventIcon /> },
            ] },
    ],
    TEACHER: [
        { title: 'Docencia', items: [
                { label: 'Inicio', path: '/teacher/home', icon: <HomeIcon /> },
                { label: 'Mis secciones', path: '/teacher/sections', icon: <GradeIcon /> },
            ] },
    ],
    STUDENT: [
        { title: 'Mi espacio', items: [
                { label: 'Inicio', path: '/student/home', icon: <HomeIcon /> },
                { label: 'Plan de estudios', path: '/student/plan', icon: <MenuBookIcon /> },
                { label: 'Oferta académica', path: '/student/offer', icon: <ViewListIcon /> },
                { label: 'Inscripción', path: '/student/enroll', icon: <AppRegistrationIcon /> },
                { label: 'Historial académico', path: '/student/history', icon: <HistoryEduIcon /> },
            ] },
    ],
}

const ROLE_NAMES = { ADMIN: 'Administrador Académico', TEACHER: 'Docente', STUDENT: 'Estudiante' }

function SidebarContent({ user, onNavigate, onLogout }) {
    const location = useLocation()
    return (
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: NAVY, color: '#fff' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, px: 2.5, py: 3 }}>
                <Box sx={{ bgcolor: '#0f766e', borderRadius: 2, width: 40, height: 40, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <SchoolIcon sx={{ color: '#fff' }} />
                </Box>
                <Box>
                    <Typography sx={{ fontWeight: 800, fontSize: 18, lineHeight: 1.1, letterSpacing: 0.5 }}>SIGA IPT</Typography>
                    <Typography sx={{ fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>Gestión académica</Typography>
                </Box>
            </Box>

            <Box sx={{ flexGrow: 1, overflow: 'auto', px: 1.5 }}>
                {MENU[user.role].map((group) => (
                    <Box key={group.title} sx={{ mb: 2 }}>
                        <Typography sx={{ px: 1.5, mb: 0.5, fontSize: 11, fontWeight: 700, letterSpacing: 1, textTransform: 'uppercase', color: 'rgba(255,255,255,0.45)' }}>
                            {group.title}
                        </Typography>
                        <List disablePadding>
                            {group.items.map((item) => {
                                const active = location.pathname.startsWith(item.path)
                                return (
                                    <ListItemButton
                                        key={item.path}
                                        onClick={() => onNavigate(item.path)}
                                        sx={{
                                            borderRadius: 2,
                                            mb: 0.5,
                                            color: active ? '#fff' : 'rgba(255,255,255,0.72)',
                                            bgcolor: active ? 'rgba(45,212,191,0.16)' : 'transparent',
                                            '&:hover': { bgcolor: 'rgba(255,255,255,0.08)' },
                                        }}
                                    >
                                        <ListItemIcon sx={{ minWidth: 38, color: active ? TEAL : 'rgba(255,255,255,0.55)' }}>{item.icon}</ListItemIcon>
                                        <ListItemText primary={item.label} slotProps={{ primary: { fontSize: 14, fontWeight: active ? 700 : 500 } }} />
                                    </ListItemButton>
                                )
                            })}
                        </List>
                    </Box>
                ))}
            </Box>

            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, p: 2, borderTop: '1px solid rgba(255,255,255,0.12)' }}>
                <Avatar sx={{ bgcolor: '#0f766e', width: 36, height: 36, fontSize: 15 }}>{user.name?.[0]?.toUpperCase()}</Avatar>
                <Box sx={{ minWidth: 0, flexGrow: 1 }}>
                    <Typography noWrap sx={{ fontSize: 13, fontWeight: 600 }}>{user.name}</Typography>
                    <Typography noWrap sx={{ fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>{ROLE_NAMES[user.role]}</Typography>
                </Box>
                <Tooltip title="Cerrar sesión">
                    <IconButton size="small" sx={{ color: 'rgba(255,255,255,0.8)' }} onClick={onLogout}>
                        <LogoutIcon fontSize="small" />
                    </IconButton>
                </Tooltip>
            </Box>
        </Box>
    )
}

export default function Layout() {
    const { user, logout } = useAuth()
    const navigate = useNavigate()
    const theme = useTheme()
    const isDesktop = useMediaQuery(theme.breakpoints.up('md'))
    const [mobileOpen, setMobileOpen] = useState(false)

    if (!user) return <Navigate to="/login" replace />

    const handleNavigate = (path) => {
        setMobileOpen(false)
        navigate(path)
    }
    const handleLogout = () => {
        logout()
        navigate('/login')
    }
    const sidebar = <SidebarContent user={user} onNavigate={handleNavigate} onLogout={handleLogout} />

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh' }}>
            {isDesktop ? (
                <Drawer variant="permanent" sx={{ width: DRAWER_WIDTH, flexShrink: 0, '& .MuiDrawer-paper': { width: DRAWER_WIDTH, border: 0 } }}>
                    {sidebar}
                </Drawer>
            ) : (
                <>
                    <AppBar position="fixed" elevation={0} sx={{ bgcolor: NAVY }}>
                        <Toolbar>
                            <IconButton color="inherit" edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 1 }}><MenuIcon /></IconButton>
                            <Typography sx={{ fontWeight: 800 }}>SIGA IPT</Typography>
                        </Toolbar>
                    </AppBar>
                    <Drawer open={mobileOpen} onClose={() => setMobileOpen(false)} sx={{ '& .MuiDrawer-paper': { width: DRAWER_WIDTH, border: 0 } }}>
                        {sidebar}
                    </Drawer>
                </>
            )}
            <Box component="main" sx={{ flexGrow: 1, minWidth: 0, p: { xs: 2, md: 4 } }}>
                {!isDesktop && <Toolbar />}
                <Box sx={{ maxWidth: 1240, mx: 'auto' }}>
                    <Outlet />
                </Box>
            </Box>
        </Box>
    )
}