import { useEffect, useState } from 'react'
import { MenuItem, TextField } from '@mui/material'
import { courseService, periodService, sectionService, studentService } from '../../api/services'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../components/useApi'
import { PageHeader, SimpleTable } from '../../components/Common'

// Academic offer of a period, limited to the courses of the student's study plan
export default function StudentOfferPage() {
  const { user } = useAuth()
  const { data: periods } = useApi(() => periodService.getAll(), [])
  const { data: student } = useApi(() => studentService.get(user.userId), [user.userId], null)
  const { data: planCourses } = useApi(() => (student ? courseService.getByPlan(student.studyPlanId) : null), [student])
  const [periodId, setPeriodId] = useState('')

  useEffect(() => {
    if (periods.length && !periodId) {
      const open = periods.find((p) => p.status === 'OPEN')
      setPeriodId((open ?? periods[periods.length - 1]).id)
    }
  }, [periods, periodId])

  const { data: sections, loading } = useApi(() => (periodId ? sectionService.getByPeriod(periodId) : null), [periodId])
  const planIds = planCourses.map((c) => c.id)
  const offer = sections.filter((s) => planIds.includes(s.courseId))

  return (
    <>
      <PageHeader title="Oferta académica">
        <TextField select size="small" label="Período" sx={{ minWidth: 160 }} value={periodId} onChange={(e) => setPeriodId(e.target.value)}>
          {periods.map((p) => <MenuItem key={p.id} value={p.id}>{p.label}</MenuItem>)}
        </TextField>
      </PageHeader>
      <SimpleTable
        loading={loading}
        rows={offer}
        emptyText="No hay oferta para tu plan en este período"
        columns={[
          { label: 'Código', render: (r) => r.courseCode },
          { label: 'Asignatura', render: (r) => r.courseName },
          { label: 'Docente', render: (r) => r.teacherName },
          { label: 'Horario', render: (r) => r.schedule.join(', ') },
          { label: 'Cupos disponibles', render: (r) => r.availableSeats },
        ]}
      />
    </>
  )
}
