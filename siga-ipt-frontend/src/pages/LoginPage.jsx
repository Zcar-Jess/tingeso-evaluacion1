import { useState } from 'react'
import { Alert, Box, Button, Card, CardContent, MenuItem, TextField, Typography } from '@mui/material'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import useApi from '../components/useApi'
import { studentService, teacherService } from '../api/services'

// TEMPORARY login (role simulation). Keycloak will replace this page.
export default function LoginPage() {
    const { user, login, mode } = useAuth()
    const navigate = useNavigate()
    const [role, setRole] = useState('ADMIN')
    const [personId, setPersonId] = useState('')

    const { data: students } = useApi(() => (role === 'STUDENT' ? studentService.getAll() : null), [role])
    const { data: teachers } = useApi(() => (role === 'TEACHER' ? teacherService.getAll() : null), [role])

    if (user || mode === 'keycloak') return <Navigate to="/" replace />

    const people = role === 'STUDENT' ? students : role === 'TEACHER' ? teachers : []

    const handleLogin = () => {
        if (role === 'ADMIN') {
            login({ role, userId: null, name: 'Administrador' })
        } else {
            const person = people.find((p) => p.id === personId)
            login({ role, userId: person.id, name: person.fullName })
        }
        navigate('/')
    }

    return (
        <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Card sx={{ width: 420 }}>
                <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Typography variant="h5" sx={{ fontWeight: 600 }}>SIGA IPT</Typography>
                    <Alert severity="info">Acceso de desarrollo: elige un rol. Se reemplazará por Keycloak.</Alert>
                    <TextField select label="Rol" size="small" value={role} onChange={(e) => { setRole(e.target.value); setPersonId('') }}>
                        <MenuItem value="ADMIN">Administrador Académico</MenuItem>
                        <MenuItem value="TEACHER">Docente</MenuItem>
                        <MenuItem value="STUDENT">Estudiante</MenuItem>
                    </TextField>
                    {role !== 'ADMIN' && (
                        <TextField select label={role === 'TEACHER' ? 'Docente' : 'Estudiante'} size="small" value={personId} onChange={(e) => setPersonId(e.target.value)}>
                            {people.map((p) => (
                                <MenuItem key={p.id} value={p.id}>{p.fullName} ({p.run})</MenuItem>
                            ))}
                        </TextField>
                    )}
                    <Button variant="contained" disabled={role !== 'ADMIN' && !personId} onClick={handleLogin}>Ingresar</Button>
                </CardContent>
            </Card>
        </Box>
    )
}