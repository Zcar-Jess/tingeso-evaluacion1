export const STUDENT_STATUS = {
  REGULAR: 'Regular',
  POSTPONED: 'Postergación',
  TEMPORARY_WITHDRAWAL: 'Retiro temporal',
  GRADUATED: 'Egresado',
  ELIMINATED: 'Eliminado',
}
// GRADUATED and ELIMINATED are assigned by the system when a period is closed
export const MANUAL_STUDENT_STATUS = ['REGULAR', 'POSTPONED', 'TEMPORARY_WITHDRAWAL']
export const CAREER_STATUS = { ACTIVE: 'Activa', INACTIVE: 'Inactiva' }
export const TEACHER_STATUS = { ACTIVE: 'Activo', INACTIVE: 'Inactivo' }
export const PERIOD_STATUS = { OPEN: 'Abierto', CLOSED: 'Cerrado' }
export const RESULT = { PASSED: 'Aprobado', FAILED: 'Reprobado' }

export const DAYS = ['L', 'M', 'W', 'J', 'V']
export const DAY_NAMES = { L: 'Lunes', M: 'Martes', W: 'Miércoles', J: 'Jueves', V: 'Viernes' }
export const MODULES = {
  1: '08:15 - 09:35',
  2: '09:50 - 11:10',
  3: '11:25 - 12:45',
  4: '13:45 - 15:05',
  5: '15:20 - 16:40',
  6: '16:55 - 18:15',
}

export const optionsFrom = (map, keys) =>
  (keys || Object.keys(map)).map((value) => ({ value, label: map[value] }))

export const statusColor = (value) => {
  if (['ACTIVE', 'REGULAR', 'OPEN', 'PASSED', 'GRADUATED'].includes(value)) return 'success'
  if (['POSTPONED', 'TEMPORARY_WITHDRAWAL'].includes(value)) return 'warning'
  if (['INACTIVE', 'ELIMINATED', 'FAILED'].includes(value)) return 'error'
  return 'default'
}
