import { useEffect, useState } from 'react'
import { Alert, Button, Checkbox, MenuItem, TextField, Typography } from '@mui/material'
import { enrollmentService, periodService } from '../../api/services'
import { errorMessage } from '../../api/http-common'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../components/useApi'
import { useNotify } from '../../components/Notify'
import { ConfirmDialog, PageHeader, SimpleTable } from '../../components/Common'

export default function StudentEnrollPage() {
    const { user } = useAuth()
    const notify = useNotify()
    const { data: periods } = useApi(() => periodService.getAll(), [])
    const [periodId, setPeriodId] = useState('')
    const [selected, setSelected] = useState([]) // course ids
    const [toCancel, setToCancel] = useState(null)

    useEffect(() => {
        if (periods.length && !periodId) {
            const open = periods.find((p) => p.status === 'OPEN')
            setPeriodId((open ?? periods[periods.length - 1]).id)
        }
    }, [periods, periodId])

    const period = periods.find((p) => p.id === periodId)
    const isOpen = period?.status === 'OPEN'

    const { data: enrollments, loading: loadingEnrollments, reload: reloadEnrollments } = useApi(
        () => (periodId ? enrollmentService.getByStudent(user.userId, periodId) : null),
        [periodId, user.userId],
    )
    const { data: available, loading, reload: reloadAvailable } = useApi(
        () => (periodId && isOpen ? enrollmentService.getAvailableSections(user.userId, periodId) : null),
        [periodId, isOpen, user.userId],
    )

    const toggle = (courseId) =>
        setSelected((s) => (s.includes(courseId) ? s.filter((id) => id !== courseId) : [...s, courseId]))

    const refresh = () => { setSelected([]); reloadEnrollments(); reloadAvailable() }

    const enroll = async () => {
        try {
            await enrollmentService.create(periodId, { studentId: user.userId, courseIds: selected })
            notify('Inscripción realizada')
            refresh()
        } catch (e) {
            notify(errorMessage(e), 'error')
        }
    }

    const cancel = async () => {
        try {
            await enrollmentService.cancel(toCancel.id)
            notify('Inscripción anulada')
            refresh()
        } catch (e) {
            notify(errorMessage(e), 'error')
        }
        setToCancel(null)
    }

    return (
        <>
            <PageHeader title="Inscripción de asignaturas">
                <TextField select size="small" label="Período" sx={{ minWidth: 160, ml: 3 }} value={periodId} onChange={(e) => { setPeriodId(e.target.value); setSelected([]) }}>
                    {periods.map((p) => <MenuItem key={p.id} value={p.id}>{p.label}</MenuItem>)}
                </TextField>
            </PageHeader>

            {!isOpen && period && <Alert severity="info" sx={{ mb: 2 }}>El período {period.label} está cerrado: no admite inscripciones ni anulaciones.</Alert>}

            {isOpen && (
                <>
                    <Typography variant="h6" sx={{ mb: 1 }}>Asignaturas disponibles</Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>
                        Selecciona entre 3 y 6 asignaturas (o las que te falten si te quedan menos de 3).
                    </Typography>
                    <SimpleTable
                        loading={loading}
                        rows={available}
                        emptyText="No tienes asignaturas disponibles para inscribir"
                        columns={[
                            { label: '', render: (r) => <Checkbox size="small" checked={selected.includes(r.courseId)} onChange={() => toggle(r.courseId)} /> },
                            { label: 'Código', render: (r) => r.courseCode },
                            { label: 'Asignatura', render: (r) => r.courseName },
                            { label: 'Docente', render: (r) => r.teacherName },
                            { label: 'Horario', render: (r) => r.schedule.join(', ') },
                            { label: 'Cupos', render: (r) => r.availableSeats },
                        ]}
                    />
                    <Button sx={{ mt: 2, mb: 4 }} variant="contained" disabled={selected.length === 0} onClick={enroll}>
                        Inscribir {selected.length} asignatura(s)
                    </Button>
                </>
            )}

            <Typography variant="h6" sx={{ mb: 1, mt: isOpen ? 0 : 2 }}>Mis inscripciones del período</Typography>
            <SimpleTable
                loading={loadingEnrollments}
                rows={enrollments}
                emptyText="Aún no tienes inscripciones en este período"
                columns={[
                    { label: 'Código', render: (r) => r.courseCode },
                    { label: 'Asignatura', render: (r) => r.courseName },
                    { label: 'Fecha', render: (r) => r.enrollmentDate },
                    { label: 'Nota final', render: (r) => r.finalGrade ?? '—' },
                    { label: '', render: (r) => <Button size="small" color="error" disabled={!isOpen} onClick={() => setToCancel(r)}>Anular</Button> },
                ]}
            />
            <ConfirmDialog
                open={!!toCancel}
                title="Anular inscripción"
                text={`¿Anular la inscripción de ${toCancel?.courseName}?`}
                onCancel={() => setToCancel(null)}
                onConfirm={cancel}
            />
        </>
    )
}