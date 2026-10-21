import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/session': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/logout': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/statement': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
