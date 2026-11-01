import { useState } from 'react'
import { Button, IconButton, Tooltip } from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import AddIcon from '@mui/icons-material/Add'
import { useNavigate } from 'react-router-dom'
import { careerService } from '../../api/services'
import { errorMessage } from '../../api/http-common'
import useApi from '../../components/useApi'
import { useNotify } from '../../components/Notify'
import FormDialog from '../../components/FormDialog'
import { ConfirmDialog, PageHeader, SimpleTable, StatusChip } from '../../components/Common'
import { CAREER_STATUS } from '../../utils/labels'

const FIELDS = [
  { name: 'code', label: 'Código', required: true },
  { name: 'name', label: 'Nombre', required: true },
  { name: 'description', label: 'Descripción', required: true },
]

export default function CareersPage() {
  const notify = useNotify()
  const navigate = useNavigate()
  const { data: careers, loading, reload } = useApi(() => careerService.getAll(), [])
  const [dialog, setDialog] = useState(null)
  const [toDelete, setToDelete] = useState(null)

  const toggleStatus = async (c) => {
    try {
      await careerService.changeStatus(c.id, c.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      notify('Estado actualizado')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
  }

  const handleDelete = async () => {
    try {
      await careerService.remove(toDelete.id)
      notify('Carrera eliminada')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
    setToDelete(null)
  }

  return (
    <>
      <PageHeader title="Carreras">
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ type: 'create' })}>Nueva carrera</Button>
      </PageHeader>

      <SimpleTable
        loading={loading}
        rows={careers}
        columns={[
          { label: 'Código', render: (r) => r.code },
          { label: 'Nombre', render: (r) => r.name },
          { label: 'Descripción', render: (r) => r.description },
          { label: 'Estado', render: (r) => <StatusChip value={r.status} labels={CAREER_STATUS} /> },
          {
            label: 'Acciones',
            render: (r) => (
              <>
                <Button size="small" onClick={() => navigate(`/admin/careers/${r.id}/plans`)}>Planes</Button>
                <Tooltip title="Editar"><IconButton size="small" onClick={() => setDialog({ type: 'edit', row: r })}><EditIcon fontSize="small" /></IconButton></Tooltip>
                <Tooltip title={r.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}><IconButton size="small" onClick={() => toggleStatus(r)}><SwapHorizIcon fontSize="small" /></IconButton></Tooltip>
                <Tooltip title="Eliminar"><IconButton size="small" color="error" onClick={() => setToDelete(r)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
              </>
            ),
          },
        ]}
      />

      <FormDialog
        open={dialog?.type === 'create'}
        title="Nueva carrera"
        fields={FIELDS}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await careerService.create(v); notify('Carrera creada'); reload() }}
      />
      <FormDialog
        open={dialog?.type === 'edit'}
        title="Editar carrera"
        fields={FIELDS}
        initialValues={dialog?.row}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await careerService.update(dialog.row.id, v); notify('Carrera actualizada'); reload() }}
      />
      <ConfirmDialog
        open={!!toDelete}
        title="Eliminar carrera"
        text={`¿Eliminar la carrera ${toDelete?.name}? Solo es posible si no tiene estudiantes ni planes.`}
        onCancel={() => setToDelete(null)}
        onConfirm={handleDelete}
      />
    </>
  )
}
