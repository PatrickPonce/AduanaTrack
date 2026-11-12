#!/usr/bin/env node
/**
 * Prueba de extremo a extremo del flujo completo del despacho (T-00.16 · SCRUM-103).
 *
 * Recorre la API real (base de datos, Flyway, seguridad con cookie de sesión y CSRF) igual que lo hace la
 * pantalla: apertura → documentos → clasificación → liquidación y pago → DAM → canal → (inspección) → levante
 * → cita de retiro, y comprueba en cada paso el estado del despacho y, al final, su línea de tiempo, la
 * notificación del canal y la exportación del reporte.
 *
 * Escenarios:
 *   verde  canal verde: no hay inspección y el despacho pasa de CANAL_ASIGNADO a LEVANTE_OTORGADO.
 *   rojo   canal rojo: cita de inspección, resultado conforme (contrato C7) y levante. Si la inspección
 *          (T-12.1 · SCRUM-82) todavía no está publicada, comprueba la advertencia del levante (HU-13 · CA2)
 *          y registra el levante confirmando sin requisitos.
 *
 * Uso (Node 20 o superior, sin dependencias):
 *   E2E_PASSWORD=... node e2e/flujo-despacho.mjs
 * Variables:
 *   E2E_URL         URL de la aplicación (por defecto http://localhost, el docker compose local)
 *   E2E_USUARIO     usuario con rol AGENTE o ADMIN (por defecto «agente»)
 *   E2E_PASSWORD    contraseña de ese usuario (obligatoria; no se guarda en el repositorio)
 *   E2E_ESCENARIOS  escenarios separados por coma (por defecto «verde,rojo»)
 */

const BASE = (process.env.E2E_URL ?? 'http://localhost').replace(/\/+$/, '')
const API = `${BASE}/api/v1`
const USUARIO = process.env.E2E_USUARIO ?? 'agente'
const PASSWORD = process.env.E2E_PASSWORD
const ESCENARIOS = (process.env.E2E_ESCENARIOS ?? 'verde,rojo').split(',').map((e) => e.trim().toLowerCase())

const PDF = new TextEncoder().encode('%PDF-1.4\n1 0 obj << /Type /Catalog >> endobj\ntrailer << /Root 1 0 R >>\n%%EOF\n')
const DOCUMENTOS_OBLIGATORIOS = ['FACTURA_COMERCIAL', 'CONOCIMIENTO_EMBARQUE', 'LISTA_EMPAQUE', 'POLIZA_SEGURO']
const HITOS_COMUNES = [
  'APERTURA',
  'DOCUMENTACION_COMPLETA',
  'CLASIFICADO',
  'LIQUIDADO',
  'TRIBUTOS_PAGADOS',
  'DAM_NUMERADA',
  'CANAL_ASIGNADO',
  'LEVANTE_OTORGADO',
  'CITA_REGISTRADA',
]

// ---------------------------------------------------------------------------------------------------------
// Cliente HTTP con cookies (AT_SESSION y XSRF-TOKEN), como el navegador con Axios.
// ---------------------------------------------------------------------------------------------------------

const cookies = new Map()

function guardarCookies(respuesta) {
  for (const linea of respuesta.headers.getSetCookie?.() ?? []) {
    const [par, ...atributos] = linea.split(';')
    const igual = par.indexOf('=')
    const nombre = par.slice(0, igual).trim()
    const valor = par.slice(igual + 1).trim()
    const expira = atributos.some((a) => /^\s*max-age=0\s*$/i.test(a))
    if (expira || valor === '') cookies.delete(nombre)
    else cookies.set(nombre, valor)
  }
}

class ErrorApi extends Error {
  constructor(metodo, ruta, status, cuerpo) {
    super(`${metodo} ${ruta} respondió ${status}: ${typeof cuerpo === 'string' ? cuerpo : JSON.stringify(cuerpo)}`)
    this.status = status
    this.cuerpo = cuerpo
  }
}

/**
 * Llama a la API y devuelve { status, cuerpo, headers }. `esperado` es el código (o códigos) aceptado; con
 * otro código lanza ErrorApi. `json` viaja como application/json y `form` como multipart/form-data.
 */
async function api(metodo, ruta, { json, form, esperado = [200, 201, 204], binario = false } = {}) {
  const cabeceras = { Accept: 'application/json' }
  if (cookies.size) cabeceras.Cookie = [...cookies].map(([n, v]) => `${n}=${v}`).join('; ')
  if (cookies.has('XSRF-TOKEN')) cabeceras['X-XSRF-TOKEN'] = decodeURIComponent(cookies.get('XSRF-TOKEN'))
  let body
  if (json !== undefined) {
    cabeceras['Content-Type'] = 'application/json'
    body = JSON.stringify(json)
  } else if (form) {
    body = form
  }
  const respuesta = await fetch(`${API}${ruta}`, { method: metodo, headers: cabeceras, body, redirect: 'manual' })
  guardarCookies(respuesta)
  const tipo = respuesta.headers.get('content-type') ?? ''
  let cuerpo = null
  if (binario && respuesta.ok) cuerpo = new Uint8Array(await respuesta.arrayBuffer())
  else if (tipo.includes('json')) cuerpo = await respuesta.json()
  else if (respuesta.status !== 204) cuerpo = await respuesta.text()
  const aceptados = Array.isArray(esperado) ? esperado : [esperado]
  if (!aceptados.includes(respuesta.status)) throw new ErrorApi(metodo, ruta, respuesta.status, cuerpo)
  return { status: respuesta.status, cuerpo, headers: respuesta.headers }
}

function multipart(campos) {
  const form = new FormData()
  for (const [nombre, valor] of Object.entries(campos)) {
    if (valor === undefined || valor === null) continue
    if (valor instanceof Uint8Array) form.append(nombre, new Blob([valor], { type: 'application/pdf' }), `${nombre}.pdf`)
    else if (typeof valor === 'object') form.append(nombre, new Blob([JSON.stringify(valor)], { type: 'application/json' }))
    else form.append(nombre, String(valor))
  }
  return form
}

// ---------------------------------------------------------------------------------------------------------
// Datos de prueba
// ---------------------------------------------------------------------------------------------------------

/** Fecha de hoy en Lima (AAAA-MM-DD): las reglas de «no futura» del backend usan esa zona. */
function hoyEnLima(desplazamientoDias = 0) {
  const fecha = new Date(Date.now() + desplazamientoDias * 86_400_000)
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(fecha)
}

const azar = (digitos) => String(Math.floor(Math.random() * 10 ** digitos)).padStart(digitos, '0')

/** RUC 20 + 8 dígitos al azar + dígito verificador (mismo algoritmo que RucValidator). */
function rucValido() {
  const factores = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2]
  const base = `20${azar(8)}`
  const suma = [...base].reduce((acc, d, i) => acc + Number(d) * factores[i], 0)
  let digito = 11 - (suma % 11)
  if (digito === 10) digito = 0
  if (digito === 11) digito = 1
  return `${base}${digito}`
}

// ---------------------------------------------------------------------------------------------------------
// Registro de pasos
// ---------------------------------------------------------------------------------------------------------

const resultados = []

function comprobar(condicion, mensaje) {
  if (!condicion) throw new Error(mensaje)
}

async function paso(escenario, nombre, accion) {
  const inicio = performance.now()
  try {
    const detalle = await accion()
    const ms = Math.round(performance.now() - inicio)
    resultados.push({ escenario, nombre, ok: true, detalle })
    console.log(`  ✔ ${nombre}${detalle ? ` · ${detalle}` : ''} (${ms} ms)`)
  } catch (error) {
    resultados.push({ escenario, nombre, ok: false, detalle: error.message })
    console.log(`  ✘ ${nombre}\n      ${error.message}`)
    throw error
  }
}

async function estadoDe(despachoId) {
  return (await api('GET', `/despachos/${despachoId}`)).cuerpo.estado
}

// ---------------------------------------------------------------------------------------------------------
// Pasos compartidos: de la apertura al canal
// ---------------------------------------------------------------------------------------------------------

async function prepararCatalogos() {
  const regimenes = (await api('GET', '/catalogos/regimenes')).cuerpo
  const aduanas = (await api('GET', '/catalogos/aduanas')).cuerpo
  const depositos = (await api('GET', '/catalogos/depositos')).cuerpo
  let partida = null
  for (const q of ['8471', 'computadora', 'maquina', 'papel']) {
    const encontradas = (await api('GET', `/catalogos/partidas?q=${encodeURIComponent(q)}`)).cuerpo
    if (encontradas.length) {
      partida = encontradas[0]
      break
    }
  }
  comprobar(regimenes.length && aduanas.length, 'Faltan regímenes o aduanas en el catálogo')
  comprobar(depositos.length, 'No hay depósitos en el catálogo (V012)')
  comprobar(partida, 'No se encontró ninguna partida arancelaria en el catálogo')
  return { regimen: regimenes[0], aduana: aduanas[0], deposito: depositos[0], partida }
}

async function crearClienteConCartaPoder(escenario) {
  let cliente
  await paso(escenario, 'Registrar el importador (HU-04)', async () => {
    const ruc = rucValido()
    cliente = (
      await api('POST', '/clientes', {
        json: { ruc, razonSocial: `Importadora E2E ${escenario.toUpperCase()} ${azar(4)} SAC`, correo: 'e2e@ejemplo.pe' },
        esperado: 201,
      })
    ).cuerpo
    return `RUC ${ruc}`
  })
  await paso(escenario, 'Cargar la carta poder vigente (HU-04)', async () => {
    await api('POST', `/clientes/${cliente.id}/carta-poder`, {
      form: multipart({ archivo: PDF, fechaEmision: hoyEnLima(-30), fechaVencimiento: hoyEnLima(365) }),
      esperado: 201,
    })
  })
  return cliente
}

async function hastaCanal(escenario, catalogos, canal) {
  const cliente = await crearClienteConCartaPoder(escenario)
  let despacho

  await paso(escenario, 'Abrir el despacho (HU-03)', async () => {
    despacho = (
      await api('POST', '/despachos', {
        json: {
          clienteId: cliente.id,
          regimenId: catalogos.regimen.id,
          aduanaId: catalogos.aduana.id,
          referenciaCliente: `E2E-${azar(6)}`,
          descripcionMercancia: 'Laptops de 14 pulgadas para oficina (prueba de extremo a extremo)',
          borrador: false,
          tipoOperacion: 'IMPORTACION_DEFINITIVA',
          modalidadDespacho: 'ANTICIPADO',
        },
        esperado: 201,
      })
    ).cuerpo
    comprobar(despacho.estado === 'APERTURA', `Estado ${despacho.estado}, se esperaba APERTURA`)
    return despacho.numero
  })
  const id = despacho.id

  await paso(escenario, 'Registrar el embarque y el BL (HU-05)', async () => {
    await api('PUT', `/despachos/${id}/embarque`, {
      json: {
        numeroBl: `MAEU${azar(9)}`,
        tipoBl: 'MASTER',
        naviera: 'Maersk Line',
        buque: 'MAERSK E2E',
        numeroViaje: `${azar(3)}W`,
        puertoEmbarque: 'Shanghái',
        puertoDescarga: 'Callao',
        fechaEmbarque: hoyEnLima(-25),
        fechaEta: hoyEnLima(-2),
        contenedores: [{ numero: 'CSQU3054383', tipo: '40HC', precinto: `PR${azar(6)}`, pesoBrutoKg: 8250.5 }],
      },
    })
  })

  await paso(escenario, 'Cargar los 4 documentos obligatorios (HU-06)', async () => {
    for (const tipo of DOCUMENTOS_OBLIGATORIOS) {
      await api('POST', `/despachos/${id}/documentos/${tipo}`, { form: multipart({ archivo: PDF }), esperado: [200, 201] })
    }
    const estado = await estadoDe(id)
    comprobar(estado === 'DOCUMENTACION_COMPLETA', `Estado ${estado}, se esperaba DOCUMENTACION_COMPLETA`)
    return estado
  })

  await paso(escenario, 'Registrar los ítems y clasificar (HU-07)', async () => {
    await api('PUT', `/despachos/${id}/items`, {
      json: [
        { descripcion: 'Laptop 14" 16 GB RAM', cantidad: 20, unidad: 'UND', valorFob: 9800.0, partidaId: catalogos.partida.id },
        { descripcion: 'Cargador USB-C 65 W', cantidad: 20, unidad: 'UND', valorFob: 400.0, partidaId: catalogos.partida.id },
      ],
    })
    await api('POST', `/despachos/${id}/clasificacion`)
    const estado = await estadoDe(id)
    comprobar(estado === 'CLASIFICADO', `Estado ${estado}, se esperaba CLASIFICADO`)
    return `partida ${catalogos.partida.codigoFormateado ?? catalogos.partida.codigo}`
  })

  let total
  await paso(escenario, 'Generar la liquidación (HU-08)', async () => {
    const liquidacion = (await api('POST', `/despachos/${id}/liquidacion`, { json: { flete: 650.0, seguro: 95.5 } })).cuerpo
    comprobar(liquidacion.estado === 'LIQUIDADO', `Estado ${liquidacion.estado}, se esperaba LIQUIDADO`)
    total = liquidacion.total
    return `total USD ${total}`
  })

  await paso(escenario, 'Registrar el pago de tributos (HU-08)', async () => {
    const pago = (
      await api('POST', `/despachos/${id}/pago`, {
        json: { fecha: hoyEnLima(), monto: total, medioPago: 'TRANSFERENCIA', numeroOperacion: `OP-${azar(8)}` },
      })
    ).cuerpo
    comprobar(pago.estado === 'TRIBUTOS_PAGADOS', `Estado ${pago.estado}, se esperaba TRIBUTOS_PAGADOS`)
  })

  await paso(escenario, 'Registrar la numeración de la DAM (HU-09)', async () => {
    const numeroDam = `118-${hoyEnLima().slice(0, 4)}-10-${azar(6)}`
    await api('POST', `/despachos/${id}/dam`, {
      form: multipart({ numeroDam, fechaNumeracion: hoyEnLima(), cargo: PDF }),
      esperado: [200, 201],
    })
    const estado = await estadoDe(id)
    comprobar(estado === 'DAM_NUMERADA', `Estado ${estado}, se esperaba DAM_NUMERADA`)
    return numeroDam
  })

  const noLeidasAntes = (await api('GET', '/notificaciones/no-leidas/cantidad')).cuerpo.cantidad
  await paso(escenario, `Asignar el canal ${canal.toLowerCase()} (HU-10)`, async () => {
    const respuesta = (await api('PUT', `/despachos/${id}/canal`, { json: { canal } })).cuerpo
    comprobar(respuesta.estado === 'CANAL_ASIGNADO', `Estado ${respuesta.estado}, se esperaba CANAL_ASIGNADO`)
    comprobar(respuesta.levanteHabilitado === (canal === 'VERDE'), 'levanteHabilitado no corresponde al canal')
    return `levanteHabilitado=${respuesta.levanteHabilitado}`
  })

  await paso(escenario, 'El agente recibe la notificación del canal (HU-14 · CA2)', async () => {
    const noLeidas = (await api('GET', '/notificaciones/no-leidas/cantidad')).cuerpo.cantidad
    comprobar(noLeidas > noLeidasAntes, `El contador no subió (${noLeidasAntes} → ${noLeidas})`)
    const bandeja = (await api('GET', '/notificaciones?estado=NO_LEIDAS&tamano=50')).cuerpo.contenido
    const aviso = bandeja.find((n) => n.despachoId === id && n.tipo === 'CAMBIO_ESTADO')
    comprobar(aviso, 'No está la notificación del canal en la bandeja')
    return `${aviso.severidad} · ${aviso.mensaje}`
  })

  await paso(escenario, 'Antes del levante no se puede registrar la cita de retiro (HU-13 · CA4)', async () => {
    const { cuerpo } = await api('POST', `/despachos/${id}/citas`, { json: citaRetiro(catalogos.deposito.id), esperado: 409 })
    comprobar(cuerpo.codigo === 'CITA_RETIRO_SIN_LEVANTE', `Código ${cuerpo.codigo}`)
    comprobar((await estadoDe(id)) === 'CANAL_ASIGNADO', 'La cita rechazada cambió el estado del despacho')
    return cuerpo.codigo
  })

  return despacho
}

function citaRetiro(depositoId) {
  return {
    depositoId,
    numeroCita: `RT-${azar(8)}`,
    tipo: 'RETIRO',
    fechaHoraConfirmada: `${hoyEnLima(2)}T15:00:00`,
    transportista: { ruc: rucValido(), razonSocial: 'Transportes E2E SAC', placa: 'ABC-123', conductor: 'Juan Pérez' },
  }
}

// ---------------------------------------------------------------------------------------------------------
// Del levante al retiro
// ---------------------------------------------------------------------------------------------------------

async function levanteYRetiro(escenario, catalogos, despacho, { conAdvertencia }) {
  const id = despacho.id
  const numeroLevante = `LEV-${hoyEnLima().slice(0, 4)}-${azar(6)}`

  await paso(escenario, 'Registrar el levante con su constancia (HU-13)', async () => {
    const levante = (
      await api('POST', `/despachos/${id}/levante`, {
        form: multipart({
          datos: { numeroLevante, fechaLevante: hoyEnLima(), confirmarSinRequisitos: conAdvertencia },
          constancia: PDF,
        }),
        esperado: 201,
      })
    ).cuerpo
    comprobar(levante.estadoDespacho === 'LEVANTE_OTORGADO', `Estado ${levante.estadoDespacho}`)
    comprobar(levante.conAdvertencia === conAdvertencia, `conAdvertencia=${levante.conAdvertencia}`)
    comprobar(levante.retiroHabilitado === true, 'El retiro no quedó habilitado')
    return numeroLevante
  })

  await paso(escenario, 'Descargar la constancia del levante (HU-13 · CA3)', async () => {
    const { cuerpo } = await api('GET', `/despachos/${id}/levante/constancia`, { binario: true })
    comprobar(new TextDecoder().decode(cuerpo.slice(0, 5)) === '%PDF-', 'La constancia no es un PDF')
    return `${cuerpo.length} bytes`
  })

  await paso(escenario, 'Registrar la cita de retiro (HU-11 · CA3)', async () => {
    const cita = (await api('POST', `/despachos/${id}/citas`, { json: citaRetiro(catalogos.deposito.id), esperado: 201 })).cuerpo
    comprobar(cita.estado === 'PROGRAMADA', `Cita en estado ${cita.estado}`)
    comprobar(cita.estadoDespacho === 'RETIRO_PROGRAMADO', `Despacho en ${cita.estadoDespacho}`)
    comprobar(cita.transportista?.placa === 'ABC-123', 'No se guardaron los datos del transportista')
  })

  await paso(escenario, 'El despacho queda con levante y retiro programado', async () => {
    const final = (await api('GET', `/despachos/${id}`)).cuerpo
    comprobar(final.estado === 'RETIRO_PROGRAMADO', `Estado ${final.estado}`)
    comprobar(final.fechaLevante === hoyEnLima(), `fechaLevante ${final.fechaLevante}`)
    return `${final.numero} · ${final.estado}`
  })

  await paso(escenario, 'La línea de tiempo tiene todos los hitos', async () => {
    const hitos = (await api('GET', `/despachos/${id}/hitos`)).cuerpo.map((h) => h.tipo)
    const esperados = [...HITOS_COMUNES, ...(conAdvertencia ? ['LEVANTE_CON_ADVERTENCIA'] : [])]
    const faltan = esperados.filter((t) => !hitos.includes(t))
    comprobar(!faltan.length, `Faltan hitos: ${faltan.join(', ')}`)
    return `${hitos.length} hitos`
  })
}

// ---------------------------------------------------------------------------------------------------------
// Escenarios
// ---------------------------------------------------------------------------------------------------------

async function escenarioVerde(catalogos) {
  const despacho = await hastaCanal('verde', catalogos, 'VERDE')
  await levanteYRetiro('verde', catalogos, despacho, { conAdvertencia: false })
}

async function escenarioRojo(catalogos) {
  const despacho = await hastaCanal('rojo', catalogos, 'ROJO')
  const id = despacho.id

  await paso('rojo', 'Registrar la cita de inspección (HU-11 · CA2)', async () => {
    const cita = (
      await api('POST', `/despachos/${id}/citas`, {
        json: {
          depositoId: catalogos.deposito.id,
          numeroCita: `TK-${azar(8)}`,
          tipo: 'INSPECCION',
          fechaHoraConfirmada: `${hoyEnLima(1)}T09:30:00`,
          transportista: null,
        },
        esperado: 201,
      })
    ).cuerpo
    comprobar(cita.estadoDespacho === 'INSPECCION_PROGRAMADA', `Despacho en ${cita.estadoDespacho}`)
  })

  const items = await api('GET', '/catalogos/items-inspeccion', { esperado: [200, 404] })
  if (items.status === 200) {
    await paso('rojo', 'Registrar la inspección conforme (HU-12 · CA3)', async () => {
      const resultado = (
        await api('POST', `/despachos/${id}/inspeccion`, {
          form: multipart({
            datos: { items: items.cuerpo.map((d) => ({ descripcion: d, conforme: true, observacion: null })), observaciones: null },
            acta: PDF,
          }),
          esperado: 201,
        })
      ).cuerpo
      comprobar(resultado.resultado === 'CONFORME', `Resultado ${resultado.resultado}`)
      comprobar(resultado.levanteHabilitado === true, 'El levante no quedó habilitado')
      return resultado.estadoDespacho
    })
    await levanteYRetiro('rojo', catalogos, despacho, { conAdvertencia: false })
    return
  }

  console.log('  · La inspección (T-12.1 · SCRUM-82) aún no está publicada: se prueba el levante con advertencia.')
  await paso('rojo', 'Sin inspección conforme el levante avisa los requisitos pendientes (HU-13 · CA2)', async () => {
    const { cuerpo } = await api('POST', `/despachos/${id}/levante`, {
      form: multipart({ datos: { numeroLevante: `LEV-X-${azar(6)}`, fechaLevante: hoyEnLima(), confirmarSinRequisitos: false } }),
      esperado: 409,
    })
    comprobar(cuerpo.codigo === 'LEVANTE_REQUISITOS_PENDIENTES', `Código ${cuerpo.codigo}`)
    const pendientes = cuerpo.pendientes ?? cuerpo.datos?.pendientes ?? []
    comprobar(pendientes.includes('Inspección conforme'), `Pendientes: ${JSON.stringify(pendientes)}`)
    return pendientes.join(', ')
  })
  await levanteYRetiro('rojo', catalogos, despacho, { conAdvertencia: true })
}

async function reporteDelDia() {
  await paso('reporte', 'Exportar el reporte de gestión de hoy a Excel y PDF (HU-15 · CA3)', async () => {
    const desde = hoyEnLima()
    const excel = await api('GET', `/reportes/exportar?formato=xlsx&desde=${desde}&hasta=${desde}`, { binario: true })
    comprobar(new TextDecoder().decode(excel.cuerpo.slice(0, 2)) === 'PK', 'El Excel no es un .xlsx')
    const pdf = await api('GET', `/reportes/exportar?formato=pdf&desde=${desde}&hasta=${desde}`, { binario: true })
    comprobar(new TextDecoder().decode(pdf.cuerpo.slice(0, 5)) === '%PDF-', 'El PDF no es un PDF')
    return excel.headers.get('content-disposition')
  })
}

// ---------------------------------------------------------------------------------------------------------

async function main() {
  if (!PASSWORD) {
    console.error('Defina E2E_PASSWORD con la contraseña del usuario de prueba (no se guarda en el repositorio).')
    process.exitCode = 2
    return
  }
  console.log(`Prueba de extremo a extremo del despacho · ${BASE} · usuario ${USUARIO} · ${new Date().toLocaleString('es-PE')}`)

  let catalogos
  try {
    await paso('sesión', 'Iniciar sesión (HU-01)', async () => {
      const { cuerpo } = await api('POST', '/auth/login', { json: { usuario: USUARIO, password: PASSWORD, recordarme: false } })
      comprobar(cookies.has('AT_SESSION'), 'El login no dejó la cookie de sesión')
      return `${cuerpo.nombre ?? USUARIO} (${cuerpo.rol ?? 'sin rol'})`
    })
    await paso('sesión', 'Leer los catálogos (regímenes, aduanas, depósitos y partidas)', async () => {
      catalogos = await prepararCatalogos()
      return `depósito ${catalogos.deposito.codigo}`
    })
  } catch {
    // Sin sesión o sin catálogos no tiene sentido seguir. Se usa exitCode (y no process.exit) para que Node
    // cierre las conexiones de fetch con normalidad.
    process.exitCode = 1
    return
  }

  const escenarios = { verde: escenarioVerde, rojo: escenarioRojo }
  for (const nombre of ESCENARIOS) {
    if (!escenarios[nombre]) continue
    console.log(`\nEscenario canal ${nombre}`)
    await escenarios[nombre](catalogos).catch(() => {})
  }
  console.log('\nReporte')
  await reporteDelDia().catch(() => {})

  const fallidos = resultados.filter((r) => !r.ok)
  console.log(`\n${resultados.length - fallidos.length} de ${resultados.length} pasos correctos.`)
  if (fallidos.length) {
    console.log('Fallaron:')
    for (const f of fallidos) console.log(`  - [${f.escenario}] ${f.nombre}: ${f.detalle}`)
    process.exitCode = 1
  }
}

main()
