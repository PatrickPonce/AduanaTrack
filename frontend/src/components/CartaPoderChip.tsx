import type { EstadoCartaPoder } from '../types'
import { EtiquetaColor, type Tono } from './EtiquetaColor'

const ESTADOS: Record<EstadoCartaPoder, { texto: string; tono: Tono }> = {
  VIGENTE: { texto: 'Vigente', tono: 'verde' },
  POR_VENCER: { texto: 'Por vencer', tono: 'naranja' },
  VENCIDA: { texto: 'Vencida', tono: 'rojo' },
  SIN_CARTA: { texto: 'Sin carta', tono: 'gris' },
}

export function CartaPoderChip({ estado }: { estado: EstadoCartaPoder }) {
  return <EtiquetaColor {...(ESTADOS[estado] ?? ESTADOS.SIN_CARTA)} />
}
