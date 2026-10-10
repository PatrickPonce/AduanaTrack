/** "2026-10-20" o "2026-10-20T09:15:00" → "20/10/2026", sin desfases de zona horaria. */
export function formatoFecha(iso: string | null | undefined): string {
  if (!iso) return '—'
  const [anio, mes, dia] = iso.slice(0, 10).split('-')
  return anio && mes && dia ? `${dia}/${mes}/${anio}` : '—'
}

/** "2026-10-20T09:15:30" → "20/10/2026 09:15". */
export function formatoFechaHora(iso: string | null | undefined): string {
  if (!iso) return '—'
  const hora = iso.length >= 16 ? iso.slice(11, 16) : ''
  return `${formatoFecha(iso)}${hora ? ` ${hora}` : ''}`
}

/** Cadena vacía → null para los campos opcionales que se envían al API. */
export function vacioANulo(valor: string): string | null {
  const recortado = valor.trim()
  return recortado ? recortado : null
}
