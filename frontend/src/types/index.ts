// Tipos del contrato del API REST (/api/v1). Ver backend/README.md.

export type Rol = 'ADMIN' | 'AGENTE' | 'CLIENTE'

export interface UsuarioSesion {
  usuario: string
  nombre: string
  rol: Rol
}

export interface Pagina<T> {
  contenido: T[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export type EstadoCartaPoder = 'VIGENTE' | 'POR_VENCER' | 'VENCIDA' | 'SIN_CARTA'

export interface ClienteResumen {
  id: number
  ruc: string
  razonSocial: string
  estadoCartaPoder: EstadoCartaPoder
}

export interface Cliente extends ClienteResumen {
  direccion: string | null
  correo: string | null
  telefono: string | null
  contacto: string | null
  activo: boolean
  creadoEn: string
  actualizadoEn: string
}

export interface ClienteRequest {
  ruc: string
  razonSocial: string
  direccion: string
  correo: string
  telefono: string
  contacto: string
}

export interface ClienteExiste {
  existe: boolean
  clienteId: number | null
  razonSocial: string | null
}

export interface CambioCliente {
  campo: string
  valorAnterior: string | null
  valorNuevo: string | null
  usuario: string
  fecha: string
}

export interface Regimen {
  id: number
  codigo: string
  descripcion: string
}

export interface Aduana {
  id: number
  codigo: string
  nombre: string
}

/** Sprint 1: BORRADOR y APERTURA; los demás estados llegan en los Sprints 2 y 3. */
export type EstadoDespacho = 'BORRADOR' | 'APERTURA' | (string & {})

export interface CatalogoRef {
  id: number
  codigo: string
  descripcion: string
}

export interface Despacho {
  id: number
  numero: string | null
  estado: EstadoDespacho
  cliente: { id: number; ruc: string; razonSocial: string; estadoCartaPoder: EstadoCartaPoder }
  regimen: CatalogoRef | null
  aduana: CatalogoRef | null
  referenciaCliente: string | null
  descripcionMercancia: string | null
  canalControl: string | null
  fechaLevante: string | null
  agente: string
  creadoEn: string
}

export interface DespachoRequest {
  clienteId: number | null
  regimenId: number | null
  aduanaId: number | null
  referenciaCliente: string
  descripcionMercancia: string
  borrador: boolean
}

export type TipoBl = 'MASTER' | 'HOUSE'

export interface Contenedor {
  numero: string
  tipo: string
  precinto: string
}

export interface EmbarqueRequest {
  numeroBl: string
  tipoBl: TipoBl | ''
  naviera: string
  buque: string
  puertoEmbarque: string
  puertoDescarga: string
  fechaEmbarque: string
  fechaEta: string
  contenedores: Contenedor[]
}

export interface Embarque {
  id: number
  despachoId: number
  numeroDespacho: string
  numeroBl: string
  tipoBl: TipoBl
  naviera: string
  buque: string | null
  puertoEmbarque: string
  puertoDescarga: string | null
  fechaEmbarque: string | null
  fechaEta: string
  fechaLimiteAlmacenaje: string
  diasRestantes: number | null
  alertaAlmacenaje: boolean
  etaModificada: boolean
  etaAnterior: string | null
  contenedores: (Contenedor & { id: number; tipo: string | null; precinto: string | null })[]
}

export interface Indicadores {
  despachosActivos: number
  enCanalNaranjaRojo: number
  conLevante: number
  conAlertasCriticas: number
}

export interface FiltrosTablero {
  aduanaId?: number
  clienteId?: number
}

export interface DespachoTablero {
  id: number
  numero: string
  clienteId: number
  cliente: string
  aduanaCodigo: string | null
  aduana: string | null
  numeroBl: string | null
  fechaEta: string | null
  estado: EstadoDespacho
  diasAlmacenaje: number | null
  alertaAlmacenaje: boolean
  estadoCartaPoder: EstadoCartaPoder
  alertaCritica: boolean
}

/** Formato único de error del backend. */
export interface ApiErrorBody {
  codigo: string
  mensaje: string
  campos?: Record<string, string>
  [extra: string]: unknown
}
