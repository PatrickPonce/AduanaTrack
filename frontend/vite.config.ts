import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  server: {
    // En desarrollo el backend corre en :8080; en Docker el proxy lo hace nginx.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    // Las pruebas de página escriben en varios campos con userEvent: en equipos lentos pasan de 5 s.
    testTimeout: 15000,
  },
})
