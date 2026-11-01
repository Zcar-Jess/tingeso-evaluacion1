import { useState } from 'react'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import { useNavigate } from 'react-router-dom'
import { periodService, studentService } from '../../api/services'
import { errorMessage } from '../../api/http-common'
import useApi from '../../components/useApi'
import { useNotify } from '../../components/Notify'
import FormDialog from '../../components/FormDialog'
import { ConfirmDialog, PageHeader, SimpleTable, StatusChip } from '../../components/Common'
import { PERIOD_STATUS } from '../../utils/labels'

function RegistrationsDialog({ period, onClose }) {
    const notify = useNotify()
    const { data: registrations, loading, reload } = useApi(() => (period ? periodService.getRegistrations(period.id) : null), [period])
    const { data: students } = useApi(() => (period ? studentService.getAll({ status: 'REGULAR' }) : null), [period])
    const [studentId, setStudentId] = useState('')

    const register = async () => {
        try {
            await periodService.register(period.id, studentId)
            notify('Matrícula registrada')
            setStudentId('')
            reload()
        } catch (e) {
            notify(errorMessage(e), 'error')
        }
    }

    const registeredIds = registrations.map((r) => r.studentId)

    return (
        <Dialog open={!!period} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle>Matriculados en el período {period?.label}</DialogTitle>
            <DialogContent>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>
                    La matrícula administrativa es externa a SIGA IPT; aquí solo se registra quién está matriculado.
                </Typography>
                {period?.status === 'OPEN' && (
                    <Stack direction="row" sx={{ gap: 1, mb: 2 }}>
                        <TextField select size="small" label="Estudiante" sx={{ minWidth: 320 }} value={studentId} onChange={(e) => setStudentId(e.target.value)}>
                            {students.filter((s) => !registeredIds.includes(s.id)).map((s) => (
                                <MenuItem key={s.id} value={s.id}>{s.fullName} ({s.run})</MenuItem>
                            ))}
                        </TextField>
                        <Button variant="contained" disabled={!studentId} onClick={register}>Matricular</Button>
                    </Stack>
                )}
                <SimpleTable
                    loading={loading}
                    rows={registrations}
                    columns={[
                        { label: 'RUN', render: (r) => r.studentRun },
                        { label: 'Estudiante', render: (r) => r.studentName },
                    ]}
                />
            </DialogContent>
            <DialogActions><Button onClick={onClose}>Cerrar</Button></DialogActions>
        </Dialog>
    )
}

export default function PeriodsPage() {
    const notify = useNotify()
    const navigate = useNavigate()
    const { data: periods, loading, reload } = useApi(() => periodService.getAll(), [])
    const [creating, setCreating] = useState(false)
    const [registrationsOf, setRegistrationsOf] = useState(null)
    const [toClose, setToClose] = useState(null)
    const [closure, setClosure] = useState(null)

    const handleClose = async () => {
        try {
            const { data } = await periodService.close(toClose.id)
            setClosure(data)
            reload()
        } catch (e) {
            notify(errorMessage(e), 'error')
        }
        setToClose(null)
    }

    return (
        <>
            <PageHeader title="Períodos académicos">
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreating(true)}>Nuevo período</Button>
            </PageHeader>

            <SimpleTable
                loading={loading}
                rows={periods}
                columns={[
                    { label: 'Período', render: (r) => r.label },
                    { label: 'Estado', render: (r) => <StatusChip value={r.status} labels={PERIOD_STATUS} /> },
                    {
                        label: 'Acciones',
                        render: (r) => (
                            <>
                                <Button size="small" onClick={() => navigate(`/admin/periods/${r.id}/sections`)}>Oferta y secciones</Button>
                                <Button size="small" onClick={() => setRegistrationsOf(r)}>Matriculados</Button>
                                <Button size="small" color="warning" disabled={r.status !== 'OPEN'} onClick={() => setToClose(r)}>Cerrar período</Button>
                            </>
                        ),
                    },
                ]}
            />

            <FormDialog
                open={creating}
                title="Nuevo período académico"
                fields={[
                    { name: 'academicYear', label: 'Año', type: 'number', required: true },
                    { name: 'semester', label: 'Semestre', type: 'select', required: true, options: [{ value: 1, label: '1' }, { value: 2, label: '2' }] },
                ]}
                initialValues={{ academicYear: new Date().getFullYear() + 1, semester: 1 }}
                onClose={() => setCreating(false)}
                onSubmit={async (v) => { await periodService.create(v); notify('Período creado (ABIERTO)'); reload() }}
            />
            <RegistrationsDialog period={registrationsOf} onClose={() => setRegistrationsOf(null)} />
            <ConfirmDialog
                open={!!toClose}
                title="Cerrar período"
                text={`¿Cerrar el período ${toClose?.label}? Todas las inscripciones deben tener calificación. Esta acción no se puede deshacer.`}
                onCancel={() => setToClose(null)}
                onConfirm={handleClose}
            />
            <Dialog open={!!closure} onClose={() => setClosure(null)} fullWidth maxWidth="sm">
                <DialogTitle>Período {closure?.period?.label} cerrado</DialogTitle>
                <DialogContent>
                    <Stack spacing={1}>
                        <Alert severity="success">Registros consolidados en el historial: {closure?.recordsCreated}</Alert>
                        <Alert severity={closure?.eliminatedStudents?.length ? 'warning' : 'info'}>
                            Estudiantes eliminados: {closure?.eliminatedStudents?.join(', ') || 'ninguno'}
                        </Alert>
                        <Alert severity="info">Estudiantes egresados: {closure?.graduatedStudents?.join(', ') || 'ninguno'}</Alert>
                    </Stack>
                </DialogContent>
                <DialogActions><Button onClick={() => setClosure(null)}>Cerrar</Button></DialogActions>
            </Dialog>
        </>
    )
}