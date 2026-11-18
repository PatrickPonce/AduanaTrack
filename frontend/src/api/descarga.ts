import axios from 'axios'

const NO_DISPONIBLE = 'El archivo no está disponible en el servidor. Vuelva a cargarlo desde la aplicación.'

/** Mensaje claro para el usuario según lo que respondió el servidor al pedir el archivo. */
async function mensajeDeDescarga(error: unknown): Promise<string> {
  if (!axios.isAxiosError(error) || !error.response) return 'No se pudo conectar con el servidor.'
  const { status, data } = error.response
  if (status === 404) return NO_DISPONIBLE
  if (status === 401) return 'Su sesión expiró. Inicie sesión de nuevo.'
  if (status === 403) return 'No tiene permiso para descargar este archivo.'
  // La respuesta es un Blob: si trae JSON {mensaje}, se usa ese texto.
  if (data instanceof Blob && data.type.includes('json')) {
    try {
      const cuerpo = JSON.parse(await data.text()) as { mensaje?: string }
      if (cuerpo.mensaje) return cuerpo.mensaje
    } catch {
      // Cuerpo no legible: se usa el mensaje genérico.
    }
  }
  return 'No se pudo descargar el archivo.'
}

/** Descarga un archivo de la API con la sesión actual; si falla lanza un Error con un mensaje claro. */
export async function descargarArchivo(url: string): Promise<void> {
  let respuesta
  try {
    respuesta = await axios.get<Blob>(url, { responseType: 'blob', withCredentials: true })
  } catch (error) {
    throw new Error(await mensajeDeDescarga(error))
  }
  const disposicion = String(respuesta.headers['content-disposition'] ?? '')
  const codificado = /filename\*=UTF-8''([^;]+)/i.exec(disposicion)?.[1]
  const simple = /filename="?([^";]+)"?/i.exec(disposicion)?.[1]
  const nombre = codificado ? decodeURIComponent(codificado) : (simple ?? 'archivo')
  const objeto = URL.createObjectURL(respuesta.data)
  const enlace = document.createElement('a')
  enlace.href = objeto
  enlace.download = nombre
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(objeto)
}
