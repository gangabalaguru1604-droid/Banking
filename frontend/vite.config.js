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
      // Proxy all backend routes and rewrite redirect Location headers so the browser
      // stays seamlessly on localhost:5173 without cross-port cookie/session loss.
      '^/(login|register|logout|mfa-verify|mfa-enroll|forgot-password|reset-password|dashboard|applyLoan|pay|payPage|loan-search|customer-profile|loan-details|admin|staff|jsp-home)': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        autoRewrite: true,
        headers: {
          'X-Forwarded-Host': 'localhost:5173',
          'X-Forwarded-Port': '5173',
          'X-Forwarded-Proto': 'http'
        },
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            const location = proxyRes.headers['location'];
            if (location && location.includes('localhost:8081')) {
              proxyRes.headers['location'] = location.replace('localhost:8081', 'localhost:5173');
            }
          });
        }
      }
    }
  }
})
