import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const backend = env.VITE_BACKEND_URL || 'http://localhost:8080'
  return {
    plugins: [vue()],
    server: {
      host: 'localhost',
      proxy: {
        '/callback': { target: backend, changeOrigin: true },
        '/websocket': { target: backend, ws: true, changeOrigin: true },
      },
    },
    test: { environment: 'jsdom', globals: true },
  }
})
