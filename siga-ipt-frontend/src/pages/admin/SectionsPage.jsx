import { useEffect, useState } from 'react'
import {
    Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, MenuItem, Stack, TextField, Tooltip, Typography,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import AddIcon from '@mui/icons-material/Add'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useNavigate, useParams } from 'react-router-dom'
import { careerService, courseService, periodService, sectionService, studyPlanService, teacherService } from '../../api/services'
import { errorMessage } from '../../api/http-common'
import useApi from '../../components/useApi'
import { useNotify } from '../../components/Notify'
import { ConfirmDialog, PageHeader, SimpleTable, StatusChip } from '../../components/Common'
import ScheduleGrid from '../../components/ScheduleGrid'
import EnrollmentsDialog from '../../components/EnrollmentsDialog'
import { PERIOD_STATUS } from '../../utils/labels'

function SectionDialog({ open, section, periodId, onClose, onSaved }) {
    const notify = useNotify()
    const isEdit = !!section
    const { data: careers } = useApi(() => (open && !isEdit ? careerService.getAll() : null), [open, isEdit])
    const { data: teachers } = useApi(() => (open ? teacherService.getAll({ status: 'ACTIVE' }) : null), [open])
    const [careerId, setCareerId] = useState('')
    const [courses, setCourses] = useState([])
    const [courseId, setCourseId] = useState('')
    const [teacherId, setTeacherId] = useState('')
    const [capacity, setCapacity] = useState('')
    const [blocks, setBlocks] = useState([])

    useEffect(() => {
        if (!open) return
        setCareerId('')
        setCourses([])
        setCourseId(section?.courseId ?? '')
        setTeacherId(section?.teacherId ?? '')
        setCapacity(section?.capacity ?? '')
        setBlocks(section?.scheduleBlocks ?? [])
    }, [open, section])

    // Courses offered for a career = courses of its current study plan
    useEffect(() => {
        if (!careerId) return
            ;(async () => {
            try {
                const { data: plans } = await studyPlanService.getByCareer(careerId)
                const current = plans.find((p) => p.current)
                if (!current) { setCourses([]); notify('La carrera no tiene un plan vigente', 'warning'); return }
                const { data } = await courseService.getByPlan(current.id)
                setCourses(data)
            } catch (e) {
                notify(errorMessage(e), 'error')
            }
        })()
    }, [careerId, notify])

    const selectedCourse = courses.find((c) => c.id === courseId)
    const requiredBlocks = selectedCourse
        ? (selectedCourse.theoryHours + selectedCourse.exerciseHours + selectedCourse.labHours) / 2
        : (section ? section.scheduleBlocks.length : null)

    const handleSave = async () => {
        try {
            if (isEdit) {
                await sectionService.update(section.id, { teacherId, capacity: Number(capacity), scheduleBlocks: blocks })
            } else {
                await sectionService.create(periodId, { courseId, teacherId, capacity: Number(capacity), scheduleBlocks: blocks })
            }
            notify('Sección guardada')
            onSaved()
            onClose()
        } catch (e) {
            notify(errorMessage(e), 'error')
        }
    }

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle>{isEdit ? `Editar sección — ${section.courseCode}` : 'Nueva sección'}</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ mt: 1 }}>
                    {!isEdit && (
                        <>
                            <TextField select size="small" label="Carrera" value={careerId} onChange={(e) => { setCareerId(e.target.value); setCourseId('') }}>
                                {careers.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
                            </TextField>
                            <TextField select size="small" label="Asignatura (plan vigente)" value={courseId} onChange={(e) => setCourseId(e.target.value)} disabled={!careerId}>
                                {courses.map((c) => <MenuItem key={c.id} value={c.id}>{c.code} - {c.name} (sem. {c.semester})</MenuItem>)}
                            </TextField>
                        </>
                    )}
                    <TextField select size="small" label="Docente responsable (activos)" value={teacherId} onChange={(e) => setTeacherId(e.target.value)}>
                        {teachers.map((t) => <MenuItem key={t.id} value={t.id}>{t.fullName}</MenuItem>)}
                    </TextField>
                    <TextField size="small" type="number" label="Capacidad (1 a 60)" value={capacity} onChange={(e) => setCapacity(e.target.value)} slotProps={{ htmlInput: { min: 1, max: 60 } }} />
                    <Typography variant="subtitle2">
                        Bloques horarios {requiredBlocks != null && `— requeridos: ${requiredBlocks} · seleccionados: ${blocks.length}`}
                    </Typography>
                    <ScheduleGrid value={blocks} onChange={setBlocks} />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancelar</Button>
                <Button variant="contained" onClick={handleSave} disabled={!teacherId || !capacity || (!isEdit && !courseId) || blocks.length === 0}>Guardar</Button>
            </DialogActions>
        </Dialog>
    )
}

export default function SectionsPage() {
    const { periodId } = useParams()
    const notify = useNotify()
    const navigate = useNavigate()
    const { data: period } = useApi(() => periodService.get(periodId), [periodId], null)
    const { data: sections, loading, reload } = useApi(() => sectionService.getByPeriod(periodId), [periodId])
    const [dialog, setDialog] = useState(null) // { section } (section undefined = create)
    const [toDelete, setToDelete] = useState(null)
    const [viewing, setViewing] = useState(null)

    const isOpen = period?.status === 'OPEN'

    const handleDelete = async () => {
        try {
            await sectionService.remove(toDelete.id)
            notify('Sección eliminada')
            reload()
        } catch (e) {
            notify(errorMessage(e), 'error')
        }
        setToDelete(null)
    }

    return (
        <>
            <PageHeader title={`Oferta académica — ${period?.label ?? ''}`}>
                {period && <StatusChip value={period.status} labels={PERIOD_STATUS} />}
                <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/admin/periods')}>Volver</Button>
                <Button variant="contained" startIcon={<AddIcon />} disabled={!isOpen} onClick={() => setDialog({})}>Nueva sección</Button>
            </PageHeader>

            <SimpleTable
                loading={loading}
                rows={sections}
                columns={[
                    { label: 'Código', render: (r) => r.courseCode },
                    { label: 'Asignatura', render: (r) => r.courseName },
                    { label: 'Docente', render: (r) => r.teacherName },
                    { label: 'Horario', render: (r) => r.schedule.join(', ') },
                    { label: 'Inscritos / Capacidad', render: (r) => `${r.enrolled} / ${r.capacity}` },
                    {
                        label: 'Acciones',
                        render: (r) => (
                            <>
                                <Button size="small" onClick={() => setViewing(r)}>Inscritos</Button>
                                <Tooltip title="Editar"><span><IconButton size="small" disabled={!isOpen} onClick={() => setDialog({ section: r })}><EditIcon fontSize="small" /></IconButton></span></Tooltip>
                                <Tooltip title="Eliminar"><span><IconButton size="small" color="error" disabled={!isOpen} onClick={() => setToDelete(r)}><DeleteIcon fontSize="small" /></IconButton></span></Tooltip>
                            </>
                        ),
                    },
                ]}
            />

            <SectionDialog open={!!dialog} section={dialog?.section} periodId={periodId} onClose={() => setDialog(null)} onSaved={reload} />
            <EnrollmentsDialog section={viewing} editable={false} onClose={() => setViewing(null)} />
            <ConfirmDialog
                open={!!toDelete}
                title="Eliminar sección"
                text={`¿Eliminar la sección de ${toDelete?.courseName}? Solo es posible si no tiene estudiantes inscritos.`}
                onCancel={() => setToDelete(null)}
                onConfirm={handleDelete}
            />
        </>
    )
}