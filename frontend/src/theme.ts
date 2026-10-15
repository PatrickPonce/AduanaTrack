import { createTheme } from '@mui/material'
import { esES } from '@mui/material/locale'

/** Colores de los prototipos de las HU (Grupo 2 - Historias de Usuario v2.0). */
export const COLORES = {
  marino: '#0d2a4b',
  marinoActivo: '#174a7c',
  verde: '#2e8b57',
  verdeActivo: '#34c47c',
  azul: '#1a6fdc',
  texto: '#0f1f3a',
  textoSuave: '#64748b',
  borde: '#e2e8f0',
  bordeCampo: '#d0d7e2',
  fondo: '#f4f6f9',
  encabezadoTabla: '#eef2f7',
} as const

/**
 * Las etiquetas de los campos van encima del recuadro (como en los prototipos) y no
 * flotando dentro: la etiqueta queda en el flujo y el borde no se recorta para ella.
 */
export const tema = createTheme(
  {
    palette: {
      primary: { main: COLORES.azul },
      success: { main: '#16a34a' },
      warning: { main: '#d97706' },
      error: { main: '#dc2626' },
      text: { primary: COLORES.texto, secondary: COLORES.textoSuave },
      background: { default: COLORES.fondo },
      divider: COLORES.borde,
    },
    shape: { borderRadius: 6 },
    typography: {
      fontFamily: '"Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
      fontSize: 14,
      button: { textTransform: 'none', fontWeight: 600 },
    },
    components: {
      MuiPaper: {
        defaultProps: { elevation: 0 },
        styleOverrides: { root: { border: `1px solid ${COLORES.borde}`, borderRadius: 10 } },
      },
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: { root: { borderRadius: 6, padding: '8px 16px' } },
      },
      MuiTextField: {
        defaultProps: { fullWidth: true, size: 'small' },
      },
      MuiAutocomplete: {
        defaultProps: { size: 'small' },
      },
      MuiInputLabel: {
        defaultProps: { shrink: true },
        styleOverrides: {
          root: {
            position: 'relative',
            transform: 'none',
            maxWidth: '100%',
            marginBottom: 6,
            fontSize: 12.5,
            fontWeight: 600,
            lineHeight: 1.3,
            whiteSpace: 'normal',
            color: '#334155',
            '&.Mui-focused': { color: '#334155' },
            '&.Mui-error': { color: '#dc2626' },
            '& .MuiFormLabel-asterisk': { display: 'none' },
          },
        },
      },
      MuiOutlinedInput: {
        defaultProps: { notched: false },
        styleOverrides: {
          root: {
            backgroundColor: '#fff',
            fontSize: 14,
            '& .MuiOutlinedInput-notchedOutline': { borderColor: COLORES.bordeCampo, top: 0 },
            '& .MuiOutlinedInput-notchedOutline legend': { display: 'none' },
            '&.Mui-disabled': { backgroundColor: '#f8fafc' },
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          head: {
            backgroundColor: COLORES.encabezadoTabla,
            color: '#334155',
            fontWeight: 600,
            fontSize: 13,
          },
          root: { borderColor: COLORES.borde, fontSize: 13.5 },
        },
      },
      MuiChip: {
        styleOverrides: { root: { fontWeight: 600, borderRadius: 999 } },
      },
    },
  },
  esES,
)
