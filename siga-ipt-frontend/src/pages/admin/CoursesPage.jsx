import { useState } from 'react'
import { Button, IconButton, Tooltip } from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import AddIcon from '@mui/icons-material/Add'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useNavigate, useParams } from 'react-router-dom'
import { courseService, studyPlanService } from '../../api/services'
import { errorMessage } from '../../api/http-common'
import useApi from '../../components/useApi'
import { useNotify } from '../../components/Notify'
import FormDialog from '../../components/FormDialog'
import { ConfirmDialog, PageHeader, SimpleTable } from '../../components/Common'

export default function CoursesPage() {
  const { planId } = useParams()
  const notify = useNotify()
  const navigate = useNavigate()
  const { data: plan } = useApi(() => studyPlanService.get(planId), [planId], null)
  const { data: courses, loading, reload } = useApi(() => courseService.getByPlan(planId), [planId])
  const [dialog, setDialog] = useState(null)
  const [toDelete, setToDelete] = useState(null)

  const editing = dialog?.type === 'edit' ? dialog.row : null
  const prerequisiteOptions = courses
    .filter((c) => c.id !== editing?.id)
    .map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` }))

  const fields = [
    { name: 'code', label: 'Código', required: true },
    { name: 'name', label: 'Nombre', required: true },
    { name: 'semester', label: 'Semestre (1 a 4)', type: 'number', required: true },
    { name: 'theoryHours', label: 'Horas de teoría', type: 'number', required: true },
    { name: 'exerciseHours', label: 'Horas de ejercicios', type: 'number', required: true },
    { name: 'labHours', label: 'Horas de laboratorio', type: 'number', required: true },
    { name: 'sct', label: 'SCT (1 a 7)', type: 'number', required: true },
    { name: 'prerequisiteIds', label: 'Prerrequisitos (máx. 3)', type: 'multiselect', options: prerequisiteOptions },
  ]

  const handleDelete = async () => {
    try {
      await courseService.remove(toDelete.id)
      notify('Asignatura eliminada')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
    setToDelete(null)
  }

  return (
    <>
      <PageHeader title={`Asignaturas — plan ${plan?.identifier ?? ''} (${plan?.careerCode ?? ''})`}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(`/admin/careers/${plan?.careerId}/plans`)}>Volver</Button>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ type: 'create' })}>Nueva asignatura</Button>
      </PageHeader>

      <SimpleTable
        loading={loading}
        rows={[...courses].sort((a, b) => a.semester - b.semester || a.code.localeCompare(b.code))}
        columns={[
          { label: 'Sem.', render: (r) => r.semester },
          { label: 'Código', render: (r) => r.code },
          { label: 'Asignatura', render: (r) => r.name },
          { label: 'TEL', render: (r) => `${r.theoryHours}-${r.exerciseHours}-${r.labHours}` },
          { label: 'SCT', render: (r) => r.sct },
          { label: 'Prerrequisitos', render: (r) => r.prerequisiteCodes.join(', ') || '—' },
          {
            label: 'Acciones',
            render: (r) => (
              <>
                <Tooltip title="Editar"><IconButton size="small" onClick={() => setDialog({ type: 'edit', row: r })}><EditIcon fontSize="small" /></IconButton></Tooltip>
                <Tooltip title="Eliminar"><IconButton size="small" color="error" onClick={() => setToDelete(r)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
              </>
            ),
          },
        ]}
      />

      <FormDialog
        open={dialog?.type === 'create'}
        title="Nueva asignatura"
        fields={fields}
        initialValues={{ prerequisiteIds: [] }}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await courseService.create(planId, v); notify('Asignatura creada'); reload() }}
      />
      <FormDialog
        open={dialog?.type === 'edit'}
        title="Editar asignatura"
        fields={fields}
        initialValues={dialog?.row}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await courseService.update(dialog.row.id, v); notify('Asignatura actualizada'); reload() }}
      />
      <ConfirmDialog
        open={!!toDelete}
        title="Eliminar asignatura"
        text={`¿Eliminar ${toDelete?.code} - ${toDelete?.name}? No es posible si forma parte de algún historial.`}
        onCancel={() => setToDelete(null)}
        onConfirm={handleDelete}
      />
    </>
  )
}
