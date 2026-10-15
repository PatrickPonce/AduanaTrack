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

/** Fila de la tabla "Carta poder / autorización vigente": estado según su propio vencimiento. */
export interface CartaPoderResumen {
  id: number
  fechaEmision: string
  fechaVencimiento: string
  nombreArchivo: string
  tamanoBytes: number
  estado: EstadoCartaPoder
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

export type TipoOperacion = 'IMPORTACION_DEFINITIVA' | 'ADMISION_TEMPORAL' | 'REIMPORTACION'

export type ModalidadDespacho = 'ANTICIPADO' | 'DIFERIDO' | 'URGENTE'

export interface Despacho {
  id: number
  numero: string | null
  estado: EstadoDespacho
  cliente: { id: number; ruc: string; razonSocial: string; estadoCartaPoder: EstadoCartaPoder }
  regimen: CatalogoRef | null
  aduana: CatalogoRef | null
  referenciaCliente: string | null
  descripcionMercancia: string | null
  tipoOperacion: TipoOperacion | null
  modalidadDespacho: ModalidadDespacho | null
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
  tipoOperacion: TipoOperacion | null
  modalidadDespacho: ModalidadDespacho | null
  borrador: boolean
}

export type TipoBl = 'MASTER' | 'HOUSE'

/** Fila del formulario: el peso bruto (kg) se edita como texto y se envía como número. */
export interface Contenedor {
  numero: string
  tipo: string
  precinto: string
  pesoBrutoKg: string
}

export interface EmbarqueRequest {
  numeroBl: string
  tipoBl: TipoBl | ''
  naviera: string
  buque: string
  numeroViaje: string
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
  numeroViaje: string | null
  puertoEmbarque: string
  puertoDescarga: string | null
  fechaEmbarque: string | null
  fechaEta: string
  fechaLimiteAlmacenaje: string
  diasRestantes: number | null
  alertaAlmacenaje: boolean
  etaModificada: boolean
  etaAnterior: string | null
  contenedores: {
    id: number
    numero: string
    tipo: string | null
    precinto: string | null
    pesoBrutoKg: number | null
  }[]
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
  /** Primer contenedor del BL (por número) y cuántos tiene en total. */
  contenedor: string | null
  totalContenedores: number
  fechaEta: string | null
  estado: EstadoDespacho
  /** VERDE, NARANJA o ROJO desde HU-10 (Sprint 2); nulo en el Sprint 1. */
  canalControl: string | null
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
