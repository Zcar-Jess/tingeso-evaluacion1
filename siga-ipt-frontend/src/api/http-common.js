import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  headers: { 'Content-Type': 'application/json' },
})

// The auth layer registers an async function that returns the current JWT (or null).
let tokenProvider = async () => null
export const setTokenProvider = (fn) => {
  tokenProvider = fn
}

http.interceptors.request.use(async (config) => {
  const token = await tokenProvider()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const STATUS_MESSAGES = {
  401: 'Tu sesión expiró o no estás autenticado.',
  403: 'No tienes permiso para realizar esta acción.',
}

export const errorMessage = (error) => {
  const data = error?.response?.data
  if (data?.message) {
    const details = Array.isArray(data.details) && data.details.length ? ` (${data.details.join('; ')})` : ''
    return data.message + details
  }
  const status = error?.response?.status
  if (STATUS_MESSAGES[status]) return STATUS_MESSAGES[status]
  if (error?.request && !error?.response) return 'No hay conexión con el servidor.'
  return error?.message || 'Error inesperado'
}

export default http