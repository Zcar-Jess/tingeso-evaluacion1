import { Box, Chip, Typography } from '@mui/material'
import { courseService, studentService } from '../../api/services'
import { useAuth } from '../../auth/AuthContext'
import useApi from '../../components/useApi'
import { PageHeader, SimpleTable } from '../../components/Common'

const SEMESTERS = [1, 2, 3, 4]

// Study plan of the student, grouped by semester, showing which courses are already passed.
export default function StudentPlanPage() {
    const { user } = useAuth()
    const { data: student } = useApi(() => studentService.get(user.userId), [user.userId], null)
    const { data: courses, loading } = useApi(() => (student ? courseService.getByPlan(student.studyPlanId) : null), [student])
    const { data: history } = useApi(() => studentService.history(user.userId), [user.userId])

    const passedCodes = new Set(history.filter((h) => h.result === 'PASSED').map((h) => h.courseCode))
    const passedCount = courses.filter((c) => passedCodes.has(c.code)).length

    return (
        <>
            <PageHeader
                title="Plan de estudios"
                subtitle={student ? `${student.careerName} · Plan ${student.studyPlanIdentifier}` : ''}
            >
                {courses.length > 0 && <Chip color="primary" variant="outlined" label={`${passedCount} de ${courses.length} asignaturas aprobadas`} />}
            </PageHeader>

            {SEMESTERS.map((semester) => (
                <Box key={semester} sx={{ mb: 3 }}>
                    <Typography variant="h6" sx={{ mb: 1 }}>Semestre {semester}</Typography>
                    <SimpleTable
                        loading={loading}
                        rows={courses.filter((c) => c.semester === semester)}
                        columns={[
                            { label: 'Código', render: (r) => r.code },
                            { label: 'Asignatura', render: (r) => r.name },
                            { label: 'TEL', render: (r) => `${r.theoryHours}-${r.exerciseHours}-${r.labHours}` },
                            { label: 'SCT', render: (r) => r.sct },
                            { label: 'Prerrequisitos', render: (r) => r.prerequisiteCodes.join(', ') || '—' },
                            {
                                label: 'Estado',
                                render: (r) => (passedCodes.has(r.code)
                                    ? <Chip size="small" color="success" label="Aprobada" />
                                    : <Chip size="small" variant="outlined" label="Pendiente" />),
                            },
                        ]}
                    />
                </Box>
            ))}
        </>
    )
}