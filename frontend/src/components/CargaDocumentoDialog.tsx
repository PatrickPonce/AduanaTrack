import UploadFileIcon from '@mui/icons-material/UploadFile'
import { Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from '@mui/material'
import { useEffect, useState, type ChangeEvent, type DragEvent } from 'react'

import { cargarDocumento } from '../api/documentos'
import { leerError, mensajeDeError } from '../api/http'
import type { DocumentoCargado, DocumentoRequerido } from '../types'
import { formatoTamano } from '../utils/formato'
import { ACEPTA_DOCUMENTO, validarArchivoDocumento } from '../utils/validaciones'

interface Props {
  despachoId: number
  /** Documento a cargar o reemplazar; null cierra el diálogo. */
  documento: DocumentoRequerido | null
  onCerrar: () => void
  onCargado: (cargado: DocumentoCargado) => void
}

/**
 * HU-06 · T-06.4: carga por arrastre o selección de un PDF o JPG (CA1 y regla de negocio 1). Si el documento ya tiene una
 * versión vigente, el diálogo confirma que la anterior se conserva en el historial (CA3).
 */
export function CargaDocumentoDialog({ despachoId, documento, onCerrar, onCargado }: Props) {
  const [archivo, setArchivo] = useState<File | null>(null)
  const [problema, setProblema] = useState<string | null>(null)
  const [arrastrando, setArrastrando] = useState(false)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    setArchivo(null)
    setProblema(null)
    setArrastrando(false)
  }, [documento])

  /** El tamaño y el tipo se revisan antes de enviar; el backend vuelve a validar la firma del archivo. */
  function elegir(seleccionado: File | undefined) {
    if (!seleccionado) return
    const error = validarArchivoDocumento(seleccionado)
    setProblema(error)
    setArchivo(error ? null : seleccionado)
  }

  function seleccionar(e: ChangeEvent<HTMLInputElement>) {
    elegir(e.target.files?.[0])
    e.target.value = ''
  }

  function soltar(e: DragEvent<HTMLElement>) {
    e.preventDefault()
    setArrastrando(false)
    elegir(e.dataTransfer.files[0])
  }

  async function enviar() {
    if (!documento || !archivo) return
    setEnviando(true)
    try {
      onCargado(await cargarDocumento(despachoId, documento.tipo, archivo))
    } catch (e) {
      setProblema(mensajeCarga(e))
    } finally {
      setEnviando(false)
    }
  }

  const reemplaza = documento?.version != null

  return (
    <Dialog open={documento !== null} onClose={enviando ? undefined : onCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>
        {reemplaza ? 'Reemplazar' : 'Cargar'} · {documento?.nombre}
      </DialogTitle>
      <DialogContent>
        {reemplaza && (
          <Alert severity="info" sx={{ mb: 2 }}>
            La versión vigente (v{documento!.version} · {documento!.archivo}) se conservará en el historial de
            versiones. El archivo nuevo quedará como versión {documento!.version! + 1} vigente.
          </Alert>
        )}
        <Box
          aria-label="Zona de arrastre del documento"
          onDragOver={(e) => {
            e.preventDefault()
            setArrastrando(true)
          }}
          onDragLeave={() => setArrastrando(false)}
          onDrop={soltar}
          sx={{
            display: 'grid',
            justifyItems: 'center',
            gap: 1,
            p: 3,
            border: 2,
            borderStyle: 'dashed',
            borderRadius: 2,
            borderColor: arrastrando ? 'primary.main' : 'divider',
            bgcolor: arrastrando ? 'action.hover' : 'transparent',
            textAlign: 'center',
          }}
        >
          <UploadFileIcon color="action" />
          <Typography variant="body2">Arrastre el PDF o JPG aquí o</Typography>
          <Button component="label" variant="outlined" size="small">
            Seleccionar archivo
            <input
              hidden
              type="file"
              accept={ACEPTA_DOCUMENTO}
              onChange={seleccionar}
              aria-label="Archivo del documento"
            />
          </Button>
          <Typography
            variant="body2"
            role="status"
            sx={{ fontSize: 12.5 }}
            color={problema ? 'error' : 'text.secondary'}
          >
            {problema || (archivo ? `${archivo.name} · ${formatoTamano(archivo.size)}` : 'PDF o JPG, máximo 10 MB')}
          </Typography>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCerrar} disabled={enviando}>
          Cancelar
        </Button>
        <Button variant="contained" onClick={enviar} disabled={!archivo || enviando}>
          {enviando ? 'Cargando…' : reemplaza ? 'Reemplazar documento' : 'Cargar documento'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

/** Mismos códigos que la carta poder (contrato C1): 413 ARCHIVO_MUY_GRANDE y 415 FORMATO_NO_PERMITIDO. */
function mensajeCarga(error: unknown): string {
  const { status, body } = leerError(error)
  if (status === 413) return 'El archivo supera el tamaño máximo de 10 MB.'
  if (status === 415) return 'El archivo no es un PDF ni una imagen JPG válida.'
  if (status === 400 && body?.campos?.archivo) return body.campos.archivo
  return mensajeDeError(error, 'No se pudo cargar el documento.')
}
