import { useEffect, useState } from 'react'
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material'
import { gradeService, sectionService } from '../api/services'
import { errorMessage } from '../api/http-common'
import useApi from './useApi'
import { useNotify } from './Notify'
import { SimpleTable } from './Common'

/**
 * Students enrolled in a section. When `teacherId` is given and the period is open,
 * the final grade of each student can be registered or modified.
 */
export default function EnrollmentsDialog({ section, teacherId, editable, onClose }) {
  const notify = useNotify()
  const { data: enrollments, loading, reload } = useApi(
    () => (section ? sectionService.getEnrollments(section.id) : null),
    [section],
  )
  const [grades, setGrades] = useState({})

  useEffect(() => {
    const initial = {}
    enrollments.forEach((e) => { initial[e.studentId] = e.finalGrade ?? '' })
    setGrades(initial)
  }, [enrollments])

  const saveGrade = async (enrollment) => {
    try {
      await gradeService.register(section.id, enrollment.studentId, teacherId, Number(grades[enrollment.studentId]))
      notify('Calificación registrada')
      reload()
    } catch (e) {
      notify(errorMessage(e), 'error')
    }
  }

  const columns = [
    { label: 'RUN', render: (r) => r.studentRun },
    { label: 'Estudiante', render: (r) => r.studentName },
    { label: 'Fecha inscripción', render: (r) => r.enrollmentDate },
    {
      label: 'Nota final',
      render: (r) => (editable
        ? (
          <TextField
            size="small"
            type="number"
            sx={{ width: 90 }}
            slotProps={{ htmlInput: { min: 1, max: 7, step: 0.1 } }}
            value={grades[r.studentId] ?? ''}
            onChange={(e) => setGrades((g) => ({ ...g, [r.studentId]: e.target.value }))}
          />
        )
        : (r.finalGrade ?? '—')),
    },
  ]
  if (editable) {
    columns.push({
      label: '',
      render: (r) => (
        <Button size="small" variant="outlined" disabled={grades[r.studentId] === '' || grades[r.studentId] == null} onClick={() => saveGrade(r)}>
          Guardar
        </Button>
      ),
    })
  }

  return (
    <Dialog open={!!section} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>Inscritos — {section?.courseCode} {section?.courseName} ({section?.period})</DialogTitle>
      <DialogContent>
        <SimpleTable loading={loading} rows={enrollments} columns={columns} emptyText="No hay estudiantes inscritos" />
      </DialogContent>
      <DialogActions><Button onClick={onClose}>Cerrar</Button></DialogActions>
    </Dialog>
  )
}
