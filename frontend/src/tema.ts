import { createTheme } from '@mui/material'
import { esES } from '@mui/material/locale'

/** Colores de la identidad de AduanaTrack (menú lateral azul marino y acento verde). */
export const COLORES = {
  marino: '#0f2a47',
  marinoClaro: '#173a5e',
  acento: '#2e9e62',
  borde: '#e3e8ef',
  cabeceraTabla: '#eef1f6',
  soloLectura: '#f6f8fb',
}

export const tema = createTheme(
  {
    palette: {
      primary: { main: '#1a6fd6' },
      secondary: { main: COLORES.acento },
      success: { main: '#16a34a' },
      warning: { main: '#d97706' },
      error: { main: '#dc2626' },
      background: { default: '#f4f6fa', paper: '#ffffff' },
      text: { primary: '#1f2a37', secondary: '#64748b' },
      divider: COLORES.borde,
    },
    shape: { borderRadius: 8 },
    typography: {
      fontFamily: '"Inter", "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
      h1: { fontSize: '1.5rem', fontWeight: 700 },
      h5: { fontWeight: 700 },
      h6: { fontWeight: 700 },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    components: {
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: { root: { borderRadius: 6, paddingInline: 18 } },
      },
      // Las etiquetas van encima del campo (no flotan dentro del borde).
      MuiTextField: {
        defaultProps: { size: 'small', fullWidth: true, slotProps: { inputLabel: { shrink: true } } },
      },
      MuiInputLabel: {
        styleOverrides: {
          root: {
            position: 'relative',
            transform: 'none',
            maxWidth: 'none',
            marginBottom: 6,
            fontSize: '0.8rem',
            fontWeight: 600,
            color: '#475569',
            lineHeight: 1.3,
            whiteSpace: 'normal',
          },
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: { backgroundColor: '#fff' },
          notchedOutline: { top: 0, borderColor: '#d5dce6', '& legend': { display: 'none' } },
        },
      },
      MuiFormHelperText: { styleOverrides: { root: { marginLeft: 2, marginRight: 0 } } },
      MuiPaper: { styleOverrides: { outlined: { borderColor: COLORES.borde, borderRadius: 10 } } },
      MuiTableCell: {
        styleOverrides: {
          head: { backgroundColor: COLORES.cabeceraTabla, fontWeight: 600, color: '#334155', whiteSpace: 'nowrap' },
          root: { borderColor: COLORES.borde },
        },
      },
      MuiTab: { styleOverrides: { root: { textTransform: 'none', fontWeight: 600 } } },
      MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
    },
  },
  esES,
)
