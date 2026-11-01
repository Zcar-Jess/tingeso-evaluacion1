import { useState } from 'react'
import { Button, IconButton, MenuItem, TextField, Tooltip } from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import AddIcon from '@mui/icons-material/Add'
import { teacherService } from '../../api/services'
import { errorMessage } from '../../api/http-common'
import useApi from '../../components/useApi'
import { useNotify } from '../../components/Notify'
import FormDialog from '../../components/FormDialog'
import { ConfirmDialog, PageHeader, SimpleTable, StatusChip } from '../../components/Common'
import { TEACHER_STATUS, optionsFrom } from '../../utils/labels'

const FIELDS = [
  { name: 'run', label: 'RUN', required: true },
  { name: 'fullName', label: 'Nombre completo', required: true },
  { name: 'email', label: 'Correo electrónico', required: true },
  { name: 'professionalTitle', label: 'Título profesional', required: true },
  { name: 'academicDegree', label: 'Grado académico', required: true },
]

export default function TeachersPage() {
  const notify = useNotify()
  const [statusFilter, setStatusFilter] = useState('')
  const { data: teachers, loading, reload } = useApi(
    () => teacherService.getAll(statusFilter ? { status: statusFilter } : {}),
    [statusFilter],
  )
  const [dialog, setDialog] = useState(null)
  const [toDelete, setToDelete] = useState(null)

  const toggleStatus = async (t) => {
    try {
      await teacherService.changeStatus(t.id, t.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      notify('Estado actualizado')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
  }

  const handleDelete = async () => {
    try {
      await teacherService.remove(toDelete.id)
      notify('Docente eliminado')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
    setToDelete(null)
  }

  return (
    <>
      <PageHeader title="Docentes">
        <TextField select size="small" label="Estado" sx={{ minWidth: 150 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <MenuItem value="">Todos</MenuItem>
          {optionsFrom(TEACHER_STATUS).map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
        </TextField>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ type: 'create' })}>Nuevo docente</Button>
      </PageHeader>

      <SimpleTable
        loading={loading}
        rows={teachers}
        columns={[
          { label: 'RUN', render: (r) => r.run },
          { label: 'Nombre', render: (r) => r.fullName },
          { label: 'Correo', render: (r) => r.email },
          { label: 'Título', render: (r) => r.professionalTitle },
          { label: 'Grado', render: (r) => r.academicDegree },
          { label: 'Estado', render: (r) => <StatusChip value={r.status} labels={TEACHER_STATUS} /> },
          {
            label: 'Acciones',
            render: (r) => (
              <>
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
        title="Nuevo docente"
        fields={FIELDS}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await teacherService.create(v); notify('Docente registrado'); reload() }}
      />
      <FormDialog
        open={dialog?.type === 'edit'}
        title="Editar docente"
        fields={FIELDS}
        initialValues={dialog?.row}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await teacherService.update(dialog.row.id, v); notify('Docente actualizado'); reload() }}
      />
      <ConfirmDialog
        open={!!toDelete}
        title="Eliminar docente"
        text={`¿Eliminar a ${toDelete?.fullName}? Solo es posible si no tiene secciones asociadas.`}
        onCancel={() => setToDelete(null)}
        onConfirm={handleDelete}
      />
    </>
  )
}
