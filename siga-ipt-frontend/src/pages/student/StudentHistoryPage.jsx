import { studentService } from '../../api/services'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../components/useApi'
import { PageHeader, SimpleTable, StatusChip } from '../../components/Common'
import { RESULT } from '../../utils/labels'

export default function StudentHistoryPage() {
  const { user } = useAuth()
  const { data, loading } = useApi(() => studentService.history(user.userId), [user.userId])

  return (
    <>
      <PageHeader title="Historial académico" />
      <SimpleTable
        loading={loading}
        rows={data}
        emptyText="Aún no tienes historial académico"
        columns={[
          { label: 'Período', render: (r) => r.period },
          { label: 'Código', render: (r) => r.courseCode },
          { label: 'Asignatura', render: (r) => r.courseName },
          { label: 'Nota final', render: (r) => r.finalGrade },
          { label: 'Resultado', render: (r) => <StatusChip value={r.result} labels={RESULT} /> },
        ]}
      />
    </>
  )
}
