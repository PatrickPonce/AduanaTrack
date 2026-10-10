import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import { esES } from '@mui/material/locale'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'

import { App } from './App'
import { AuthProvider } from './auth/AuthContext'

const tema = createTheme(
  {
    palette: {
      primary: { main: '#0b4f8a' },
      background: { default: '#f4f6f9' },
    },
    shape: { borderRadius: 8 },
  },
  esES,
)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={tema}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>,
)
