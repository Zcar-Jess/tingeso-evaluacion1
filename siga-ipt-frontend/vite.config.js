import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In development every request to /api is forwarded to the Spring Boot backend,
// so the browser never makes a cross-origin call (no CORS configuration needed).
// In production Nginx will do the same job.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})