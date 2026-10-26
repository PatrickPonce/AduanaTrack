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

/** Fecha local de hoy en formato AAAA-MM-DD (sin pasar por UTC). */
export function hoyIso(): string {
  const hoy = new Date()
  const mes = String(hoy.getMonth() + 1).padStart(2, '0')
  const dia = String(hoy.getDate()).padStart(2, '0')
  return `${hoy.getFullYear()}-${mes}-${dia}`
}

/** Suma días a una fecha AAAA-MM-DD y devuelve otra AAAA-MM-DD (cálculo en UTC para evitar cambios de horario). */
export function sumarDias(iso: string, dias: number): string {
  const [anio, mes, dia] = iso.split('-').map(Number)
  const fecha = new Date(Date.UTC(anio!, mes! - 1, dia! + dias))
  return fecha.toISOString().slice(0, 10)
}

/** Días entre dos fechas AAAA-MM-DD (hasta − desde). */
export function diasEntre(desde: string, hasta: string): number {
  const utc = (iso: string) => {
    const [a, m, d] = iso.split('-').map(Number)
    return Date.UTC(a!, m! - 1, d!)
  }
  return Math.round((utc(hasta) - utc(desde)) / 86_400_000)
}

/** Peso en kg con separador de miles: 18540 → "18,540 kg". */
export function formatoPeso(kg: number | null | undefined): string {
  if (kg === null || kg === undefined) return '—'
  return `${kg.toLocaleString('en-US', { maximumFractionDigits: 2 })} kg`
}

/** Tamaño de archivo legible: 482113 → "470.8 KB", 5242880 → "5.0 MB". */
export function formatoTamano(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/** Cadena vacía → null para los campos opcionales que se envían al API. */
export function vacioANulo(valor: string): string | null {
  const recortado = valor.trim()
  return recortado ? recortado : null
}
