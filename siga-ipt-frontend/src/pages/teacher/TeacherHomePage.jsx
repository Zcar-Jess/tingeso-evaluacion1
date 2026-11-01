import { useEffect, useState } from 'react'
import { Alert, Box, Button } from '@mui/material'
import ClassIcon from '@mui/icons-material/Class'
import GroupsIcon from '@mui/icons-material/Groups'
import EditNoteIcon from '@mui/icons-material/EditNote'
import EventIcon from '@mui/icons-material/Event'
import { useNavigate } from 'react-router-dom'
import { gradeService, periodService, sectionService } from '../../api/services'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../components/useApi'
import { PageHeader, StatCard } from '../../components/Common'

// Summary of the open period for a teacher: sections, enrolled students and pending final grades
export default function TeacherHomePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data: periods } = useApi(() => periodService.getAll(), [])
  const openPeriod = periods.find((p) => p.status === 'OPEN')
  const { data: sections } = useApi(
      () => (openPeriod ? gradeService.getTeacherSections(user.userId, openPeriod.id) : null),
      [openPeriod?.id, user.userId],
  )
  const [pendingGrades, setPendingGrades] = useState(0)

  useEffect(() => {
    if (sections.length === 0) {
      setPendingGrades(0)
      return undefined
    }
    let cancelled = false
    Promise.all(sections.map((s) => sectionService.getEnrollments(s.id)))
        .then((responses) => {
          const pending = responses.reduce((sum, r) => sum + r.data.filter((e) => e.finalGrade == null).length, 0)
          if (!cancelled) setPendingGrades(pending)
        })
        .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [sections])

  const enrolled = sections.reduce((sum, s) => sum + s.enrolled, 0)

  return (
      <>
        <PageHeader title={`Hola, ${user.name.split(' ')[0]}`} subtitle="Resumen de tus secciones del período abierto">
          <Button variant="contained"  onClick={() => navigate('/teacher/sections')} sx={{ ml: 5 }}>Registrar notas</Button>
        </PageHeader>

        {!openPeriod && <Alert severity="info" sx={{ mb: 3 }}>No hay un período académico abierto por ahora.</Alert>}

        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' } }}>
          <StatCard icon={<EventIcon />} label="Período" value={openPeriod ? openPeriod.label : '—'} />
          <StatCard icon={<ClassIcon />} label="Secciones" value={sections.length} />
          <StatCard icon={<GroupsIcon />} label="Estudiantes inscritos" value={enrolled} />
          <StatCard icon={<EditNoteIcon />} label="Notas pendientes" value={pendingGrades} caption={pendingGrades === 0 ? 'Todo al día' : 'Por registrar'} />
        </Box>
      </>
  )
}