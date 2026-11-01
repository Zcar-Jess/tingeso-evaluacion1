import { useEffect, useState } from 'react'
import { Button, MenuItem, TextField } from '@mui/material'
import { gradeService, periodService } from '../../api/services'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../components/useApi'
import { PageHeader, SimpleTable, StatusChip } from '../../components/Common'
import EnrollmentsDialog from '../../components/EnrollmentsDialog'
import { PERIOD_STATUS } from '../../utils/labels'

export default function TeacherSectionsPage() {
  const { user } = useAuth()
  const { data: periods } = useApi(() => periodService.getAll(), [])
  const [periodId, setPeriodId] = useState('')
  const [selected, setSelected] = useState(null)

  // Default: the open period, or the most recent one
  useEffect(() => {
    if (periods.length && !periodId) {
      const open = periods.find((p) => p.status === 'OPEN')
      setPeriodId((open ?? periods[periods.length - 1]).id)
    }
  }, [periods, periodId])

  const { data: sections, loading } = useApi(
    () => (periodId ? gradeService.getTeacherSections(user.userId, periodId) : null),
    [periodId, user.userId],
  )

  const period = periods.find((p) => p.id === periodId)

  return (
    <>
      <PageHeader title="Mis secciones">
        {period && <StatusChip value={period.status} labels={PERIOD_STATUS} />}
        <TextField select size="small" label="Período" sx={{ minWidth: 160 }} value={periodId} onChange={(e) => setPeriodId(e.target.value)}>
          {periods.map((p) => <MenuItem key={p.id} value={p.id}>{p.label}</MenuItem>)}
        </TextField>
      </PageHeader>

      <SimpleTable
        loading={loading}
        rows={sections}
        emptyText="No tienes secciones asignadas en este período"
        columns={[
          { label: 'Código', render: (r) => r.courseCode },
          { label: 'Asignatura', render: (r) => r.courseName },
          { label: 'Horario', render: (r) => r.schedule.join(', ') },
          { label: 'Inscritos / Capacidad', render: (r) => `${r.enrolled} / ${r.capacity}` },
          { label: 'Acciones', render: (r) => <Button size="small" onClick={() => setSelected(r)}>Inscritos y notas</Button> },
        ]}
      />

      <EnrollmentsDialog
        section={selected}
        teacherId={user.userId}
        editable={period?.status === 'OPEN'}
        onClose={() => setSelected(null)}
      />
    </>
  )
}
