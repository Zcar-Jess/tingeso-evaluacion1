import { useState } from 'react'
import { Button, Chip, IconButton, Tooltip } from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import AddIcon from '@mui/icons-material/Add'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useNavigate, useParams } from 'react-router-dom'
import { careerService, studyPlanService } from '../../api/services'
import { errorMessage } from '../../api/http-common'
import useApi from '../../components/useApi'
import { useNotify } from '../../components/Notify'
import FormDialog from '../../components/FormDialog'
import { ConfirmDialog, PageHeader, SimpleTable } from '../../components/Common'

export default function StudyPlansPage() {
  const { careerId } = useParams()
  const notify = useNotify()
  const navigate = useNavigate()
  const { data: career } = useApi(() => careerService.get(careerId), [careerId], null)
  const { data: plans, loading, reload } = useApi(() => studyPlanService.getByCareer(careerId), [careerId])
  const [creating, setCreating] = useState(false)
  const [toDelete, setToDelete] = useState(null)

  const setCurrent = async (plan) => {
    try {
      await studyPlanService.setCurrent(plan.id)
      notify('Plan establecido como vigente')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
  }

  const handleDelete = async () => {
    try {
      await studyPlanService.remove(toDelete.id)
      notify('Plan eliminado')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
    setToDelete(null)
  }

  return (
    <>
      <PageHeader title={`Planes de estudio — ${career?.name ?? ''}`}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/admin/careers')}>Volver</Button>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreating(true)}>Nuevo plan</Button>
      </PageHeader>

      <SimpleTable
        loading={loading}
        rows={plans}
        columns={[
          { label: 'Identificador', render: (r) => r.identifier },
          { label: 'Estado', render: (r) => <Chip size="small" color={r.current ? 'success' : 'default'} label={r.current ? 'Vigente' : 'No vigente'} /> },
          {
            label: 'Acciones',
            render: (r) => (
              <>
                <Button size="small" onClick={() => navigate(`/admin/plans/${r.id}/courses`)}>Asignaturas</Button>
                <Button size="small" disabled={r.current} onClick={() => setCurrent(r)}>Marcar vigente</Button>
                <Tooltip title="Eliminar"><IconButton size="small" color="error" onClick={() => setToDelete(r)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
              </>
            ),
          },
        ]}
      />

      <FormDialog
        open={creating}
        title="Nuevo plan de estudios"
        fields={[
          { name: 'identifier', label: 'Identificador (ej. 2027.1)', required: true },
          { name: 'current', label: 'Establecer como plan vigente', type: 'checkbox' },
        ]}
        initialValues={{ current: false }}
        onClose={() => setCreating(false)}
        onSubmit={async (v) => { await studyPlanService.create(careerId, v); notify('Plan creado'); reload() }}
      />
      <ConfirmDialog
        open={!!toDelete}
        title="Eliminar plan"
        text={`¿Eliminar el plan ${toDelete?.identifier}? Solo es posible si no tiene estudiantes asociados.`}
        onCancel={() => setToDelete(null)}
        onConfirm={handleDelete}
      />
    </>
  )
}
