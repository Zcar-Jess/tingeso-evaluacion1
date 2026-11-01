import { useState } from 'react'
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, MenuItem, TextField, Tooltip } from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import HistoryIcon from '@mui/icons-material/History'
import AddIcon from '@mui/icons-material/Add'
import { careerService, studentService } from '../../api/services'
import { errorMessage } from '../../api/http-common'
import useApi from '../../components/useApi'
import { useNotify } from '../../components/Notify'
import FormDialog from '../../components/FormDialog'
import { ConfirmDialog, PageHeader, SimpleTable, StatusChip } from '../../components/Common'
import { MANUAL_STUDENT_STATUS, RESULT, STUDENT_STATUS, optionsFrom } from '../../utils/labels'

function HistoryDialog({ student, onClose }) {
  const { data, loading } = useApi(() => (student ? studentService.history(student.id) : null), [student])
  return (
    <Dialog open={!!student} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>Historial académico — {student?.fullName}</DialogTitle>
      <DialogContent>
        <SimpleTable
          loading={loading}
          rows={data}
          columns={[
            { label: 'Período', render: (r) => r.period },
            { label: 'Código', render: (r) => r.courseCode },
            { label: 'Asignatura', render: (r) => r.courseName },
            { label: 'Nota final', render: (r) => r.finalGrade },
            { label: 'Resultado', render: (r) => <StatusChip value={r.result} labels={RESULT} /> },
          ]}
        />
      </DialogContent>
      <DialogActions><Button onClick={onClose}>Cerrar</Button></DialogActions>
    </Dialog>
  )
}

export default function StudentsPage() {
  const notify = useNotify()
  const [statusFilter, setStatusFilter] = useState('')
  const { data: students, loading, reload } = useApi(
    () => studentService.getAll(statusFilter ? { status: statusFilter } : {}),
    [statusFilter],
  )
  const { data: careers } = useApi(() => careerService.getAll(), [])
  const [dialog, setDialog] = useState(null) // { type, row }
  const [toDelete, setToDelete] = useState(null)

  const activeCareers = careers.filter((c) => c.status === 'ACTIVE').map((c) => ({ value: c.id, label: c.name }))

  const handleDelete = async () => {
    try {
      await studentService.remove(toDelete.id)
      notify('Estudiante eliminado')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
    setToDelete(null)
  }

  return (
    <>
      <PageHeader title="Estudiantes">
        <TextField select size="small" label="Estado" sx={{ minWidth: 170 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <MenuItem value="">Todos</MenuItem>
          {optionsFrom(STUDENT_STATUS).map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
        </TextField>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ type: 'create' })}>Nuevo estudiante</Button>
      </PageHeader>

      <SimpleTable
        loading={loading}
        rows={students}
        columns={[
          { label: 'RUN', render: (r) => r.run },
          { label: 'Nombre', render: (r) => r.fullName },
          { label: 'Correo', render: (r) => r.email },
          { label: 'Carrera', render: (r) => r.careerName },
          { label: 'Plan', render: (r) => r.studyPlanIdentifier },
          { label: 'Estado', render: (r) => <StatusChip value={r.status} labels={STUDENT_STATUS} /> },
          {
            label: 'Acciones',
            render: (r) => (
              <>
                <Tooltip title="Editar"><IconButton size="small" onClick={() => setDialog({ type: 'edit', row: r })}><EditIcon fontSize="small" /></IconButton></Tooltip>
                <Tooltip title="Cambiar estado"><span><IconButton size="small" disabled={['GRADUATED', 'ELIMINATED'].includes(r.status)} onClick={() => setDialog({ type: 'status', row: r })}><SwapHorizIcon fontSize="small" /></IconButton></span></Tooltip>
                <Tooltip title="Historial"><IconButton size="small" onClick={() => setDialog({ type: 'history', row: r })}><HistoryIcon fontSize="small" /></IconButton></Tooltip>
                <Tooltip title="Eliminar"><IconButton size="small" color="error" onClick={() => setToDelete(r)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
              </>
            ),
          },
        ]}
      />

      <FormDialog
        open={dialog?.type === 'create'}
        title="Nuevo estudiante"
        fields={[
          { name: 'run', label: 'RUN', required: true },
          { name: 'fullName', label: 'Nombre completo', required: true },
          { name: 'email', label: 'Correo electrónico', required: true },
          { name: 'careerId', label: 'Carrera', type: 'select', options: activeCareers, required: true },
        ]}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await studentService.create(v); notify('Estudiante registrado'); reload() }}
      />
      <FormDialog
        open={dialog?.type === 'edit'}
        title="Editar estudiante"
        fields={[
          { name: 'fullName', label: 'Nombre completo', required: true },
          { name: 'email', label: 'Correo electrónico', required: true },
        ]}
        initialValues={{ fullName: dialog?.row?.fullName, email: dialog?.row?.email }}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await studentService.update(dialog.row.id, v); notify('Estudiante actualizado'); reload() }}
      />
      <FormDialog
        open={dialog?.type === 'status'}
        title={`Cambiar estado — ${dialog?.row?.fullName ?? ''}`}
        fields={[{ name: 'status', label: 'Nuevo estado', type: 'select', options: optionsFrom(STUDENT_STATUS, MANUAL_STUDENT_STATUS), required: true }]}
        initialValues={{ status: dialog?.row?.status }}
        onClose={() => setDialog(null)}
        onSubmit={async (v) => { await studentService.changeStatus(dialog.row.id, v.status); notify('Estado actualizado'); reload() }}
      />
      <HistoryDialog student={dialog?.type === 'history' ? dialog.row : null} onClose={() => setDialog(null)} />
      <ConfirmDialog
        open={!!toDelete}
        title="Eliminar estudiante"
        text={`¿Eliminar a ${toDelete?.fullName}? Solo es posible si no tiene inscripciones ni historial.`}
        onCancel={() => setToDelete(null)}
        onConfirm={handleDelete}
      />
    </>
  )
}
