import type { Aduana, Regimen } from '../types'
import { http } from './http'

export async function listarRegimenes(): Promise<Regimen[]> {
  const { data } = await http.get<Regimen[]>('/catalogos/regimenes')
  return data
}

export async function listarAduanas(): Promise<Aduana[]> {
  const { data } = await http.get<Aduana[]>('/catalogos/aduanas')
  return data
}
