import { readFileSync } from 'node:fs'

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// La versión del pie de la barra lateral sale de package.json: se actualiza en un solo lugar.
const { version } = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf-8')) as {
  version: string
}

export default defineConfig({
  plugins: [react()],
  define: {
    __APP_VERSION__: JSON.stringify(version),
  },
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
