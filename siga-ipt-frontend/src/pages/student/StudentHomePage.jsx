import { Alert, Box, Button, Card, CardActionArea, CardContent, Typography } from '@mui/material'
import MenuBookIcon from '@mui/icons-material/MenuBook'
import ViewListIcon from '@mui/icons-material/ViewList'
import AppRegistrationIcon from '@mui/icons-material/AppRegistration'
import HistoryEduIcon from '@mui/icons-material/HistoryEdu'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import { useNavigate } from 'react-router-dom'
import { periodService, studentService } from '../../api/services'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../components/useApi'
import { InfoItem, PageHeader, SectionCard, StatusChip } from '../../components/Common'
import { STUDENT_STATUS } from '../../utils/labels'

// What a student can do in the system (actor "Estudiante" of the evaluation)
const ACTIONS = [
    { title: 'Plan de estudios', text: 'Revisa las asignaturas, créditos y prerrequisitos de tu plan.', path: '/student/plan', icon: <MenuBookIcon /> },
    { title: 'Oferta académica', text: 'Consulta las secciones, docentes y horarios del período.', path: '/student/offer', icon: <ViewListIcon /> },
    { title: 'Inscripción', text: 'Inscribe o anula asignaturas mientras el período esté abierto.', path: '/student/enroll', icon: <AppRegistrationIcon /> },
    { title: 'Historial académico', text: 'Consulta tus calificaciones y resultados por período.', path: '/student/history', icon: <HistoryEduIcon /> },
]

export default function StudentHomePage() {
    const { user } = useAuth()
    const navigate = useNavigate()
    const { data: student } = useApi(() => studentService.get(user.userId), [user.userId], null)
    const { data: periods } = useApi(() => periodService.getAll(), [])
    const openPeriod = periods.find((p) => p.status === 'OPEN')

    return (
        <>
            <PageHeader
                title={`Hola, ${user.name.split(' ')[0]}`}
                subtitle={student ? `${student.careerName} · Plan de estudios ${student.studyPlanIdentifier}` : ''}
            />

            {openPeriod ? (
                <Alert
                    severity="success"
                    sx={{ mb: 3 }}
                    action={<Button color="inherit" size="small" onClick={() => navigate('/student/enroll')}>Ir a inscripción</Button>}
                >
                    El período {openPeriod.label} está abierto: ya puedes inscribir tus asignaturas.
                </Alert>
            ) : (
                <Alert severity="info" sx={{ mb: 3 }}>No hay un período académico abierto por ahora.</Alert>
            )}

            <Typography variant="h6" sx={{ mb: 1.5 }}>¿Qué puedes hacer en SIGA IPT?</Typography>
            <Box sx={{ display: 'grid', gap: 2, mb: 4, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' } }}>
                {ACTIONS.map((a) => (
                    <Card key={a.path} sx={{ '&:hover': { borderColor: 'primary.main' } }}>
                        <CardActionArea onClick={() => navigate(a.path)} sx={{ height: '100%' }}>
                            <CardContent>
                                <Box sx={{ width: 44, height: 44, borderRadius: 2, bgcolor: 'rgba(15,118,110,0.1)', color: 'primary.main', display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 1.5 }}>
                                    {a.icon}
                                </Box>
                                <Typography sx={{ fontWeight: 700, mb: 0.5 }}>{a.title}</Typography>
                                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1.5 }}>{a.text}</Typography>
                                <Typography variant="body2" sx={{ color: 'primary.main', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                    Abrir <ArrowForwardIcon fontSize="inherit" />
                                </Typography>
                            </CardContent>
                        </CardActionArea>
                    </Card>
                ))}
            </Box>

            {student && (
                <SectionCard title="Mi información">
                    <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' } }}>
                        <InfoItem label="Nombre">{student.fullName}</InfoItem>
                        <InfoItem label="RUN">{student.run}</InfoItem>
                        <InfoItem label="Correo">{student.email}</InfoItem>
                        <InfoItem label="Carrera">{student.careerName}</InfoItem>
                        <InfoItem label="Plan de estudios">{student.studyPlanIdentifier}</InfoItem>
                        <InfoItem label="Estado"><StatusChip value={student.status} labels={STUDENT_STATUS} /></InfoItem>
                    </Box>
                </SectionCard>
            )}
        </>
    )
}