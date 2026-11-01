import http from './http-common'

const api = '/api'

export const careerService = {
  getAll: () => http.get(`${api}/careers`),
  get: (id) => http.get(`${api}/careers/${id}`),
  create: (data) => http.post(`${api}/careers`, data),
  update: (id, data) => http.put(`${api}/careers/${id}`, data),
  changeStatus: (id, status) => http.patch(`${api}/careers/${id}/status`, { status }),
  remove: (id) => http.delete(`${api}/careers/${id}`),
}

export const studyPlanService = {
  getByCareer: (careerId) => http.get(`${api}/careers/${careerId}/study-plans`),
  get: (id) => http.get(`${api}/study-plans/${id}`),
  create: (careerId, data) => http.post(`${api}/careers/${careerId}/study-plans`, data),
  setCurrent: (id) => http.patch(`${api}/study-plans/${id}/current`),
  remove: (id) => http.delete(`${api}/study-plans/${id}`),
}

export const courseService = {
  getByPlan: (planId) => http.get(`${api}/study-plans/${planId}/courses`),
  get: (id) => http.get(`${api}/courses/${id}`),
  create: (planId, data) => http.post(`${api}/study-plans/${planId}/courses`, data),
  update: (id, data) => http.put(`${api}/courses/${id}`, data),
  remove: (id) => http.delete(`${api}/courses/${id}`),
}

export const studentService = {
  getAll: (params) => http.get(`${api}/students`, { params }),
  get: (id) => http.get(`${api}/students/${id}`),
  create: (data) => http.post(`${api}/students`, data),
  update: (id, data) => http.put(`${api}/students/${id}`, data),
  changeStatus: (id, status) => http.patch(`${api}/students/${id}/status`, { status }),
  remove: (id) => http.delete(`${api}/students/${id}`),
  history: (id) => http.get(`${api}/students/${id}/academic-history`),
}

export const teacherService = {
  getAll: (params) => http.get(`${api}/teachers`, { params }),
  get: (id) => http.get(`${api}/teachers/${id}`),
  create: (data) => http.post(`${api}/teachers`, data),
  update: (id, data) => http.put(`${api}/teachers/${id}`, data),
  changeStatus: (id, status) => http.patch(`${api}/teachers/${id}/status`, { status }),
  remove: (id) => http.delete(`${api}/teachers/${id}`),
}

export const periodService = {
  getAll: () => http.get(`${api}/periods`),
  get: (id) => http.get(`${api}/periods/${id}`),
  create: (data) => http.post(`${api}/periods`, data),
  close: (id) => http.post(`${api}/periods/${id}/close`),
  getRegistrations: (id) => http.get(`${api}/periods/${id}/registrations`),
  register: (id, studentId) => http.post(`${api}/periods/${id}/registrations`, { studentId }),
}

export const sectionService = {
  getByPeriod: (periodId) => http.get(`${api}/periods/${periodId}/sections`),
  get: (id) => http.get(`${api}/sections/${id}`),
  create: (periodId, data) => http.post(`${api}/periods/${periodId}/sections`, data),
  update: (id, data) => http.put(`${api}/sections/${id}`, data),
  remove: (id) => http.delete(`${api}/sections/${id}`),
  getEnrollments: (id) => http.get(`${api}/sections/${id}/enrollments`),
}

export const enrollmentService = {
  create: (periodId, data) => http.post(`${api}/periods/${periodId}/enrollments`, data),
  cancel: (id) => http.delete(`${api}/enrollments/${id}`),
  getByStudent: (studentId, periodId) => http.get(`${api}/students/${studentId}/enrollments`, { params: { periodId } }),
  getAvailableSections: (studentId, periodId) =>
    http.get(`${api}/students/${studentId}/available-sections`, { params: { periodId } }),
}

export const gradeService = {
  register: (sectionId, studentId, teacherId, finalGrade) =>
    http.put(`${api}/sections/${sectionId}/students/${studentId}/grade`, { teacherId, finalGrade }),
  getTeacherSections: (teacherId, periodId) =>
    http.get(`${api}/teachers/${teacherId}/sections`, { params: periodId ? { periodId } : {} }),
}
