import { Alert, Box, Button } from '@mui/material'
import PeopleIcon from '@mui/icons-material/People'
import BadgeIcon from '@mui/icons-material/Badge'
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks'
import EventIcon from '@mui/icons-material/Event'
import { useNavigate } from 'react-router-dom'
import { careerService, periodService, sectionService, studentService, teacherService } from '../../api/services'
import useApi from '../../components/useApi'
import { PageHeader, StatCard } from '../../components/Common'

// Summary of the institute for the academic administrator
export default function AdminHomePage() {
  const navigate = useNavigate()
  const { data: students } = useApi(() => studentService.getAll(), [])
  const { data: teachers } = useApi(() => teacherService.getAll(), [])
  const { data: careers } = useApi(() => careerService.getAll(), [])
  const { data: periods } = useApi(() => periodService.getAll(), [])
  const openPeriod = periods.find((p) => p.status === 'OPEN')
  const { data: sections } = useApi(() => (openPeriod ? sectionService.getByPeriod(openPeriod.id) : null), [openPeriod?.id])

  return (
      <>
        <PageHeader title="Resumen" subtitle="Estado general de la gestión académica" />

        <Box sx={{ display: 'grid', gap: 2, mb: 3, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' } }}>
          <StatCard
              icon={<PeopleIcon />}
              label="Estudiantes"
              value={students.length}
              caption={`${students.filter((s) => s.status === 'REGULAR').length} regulares`}
          />
          <StatCard
              icon={<BadgeIcon />}
              label="Docentes"
              value={teachers.length}
              caption={`${teachers.filter((t) => t.status === 'ACTIVE').length} activos`}
          />
          <StatCard
              icon={<LibraryBooksIcon />}
              label="Carreras"
              value={careers.length}
              caption={`${careers.filter((c) => c.status === 'ACTIVE').length} activas`}
          />
          <StatCard
              icon={<EventIcon />}
              label="Período abierto"
              value={openPeriod ? openPeriod.label : 'Ninguno'}
              caption={openPeriod ? `${sections.length} secciones ofrecidas` : 'Crea uno para abrir la oferta'}
          />
        </Box>

        {!openPeriod && (
            <Alert severity="info" action={<Button color="inherit" size="small" onClick={() => navigate('/admin/periods')}>Ir a períodos</Button>}>
              No hay un período académico abierto. Los estudiantes no pueden inscribir asignaturas.
            </Alert>
        )}
      </>
  )
}