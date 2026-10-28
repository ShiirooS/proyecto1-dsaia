/**
 * Convierte un id en un número seguro para usar como segmento de URL. Solo acepta enteros positivos, de modo que un valor
 * manipulado (p. ej. '../user/get-all' desde la ruta del navegador) nunca altera el endpoint llamado.
 * Hallazgo de SonarCloud (tssecurity:S7044 / S8476) sobre los servicios que interpolaban el id.
 */
export function toPathId(id: unknown): number {
  const value = typeof id === 'string' ? id.trim() : id
  const parsed = typeof value === 'number' ? value : Number(value)
  if (!Number.isSafeInteger(parsed) || parsed <= 0 || String(parsed) !== String(value)) {
    throw new Error(`Identificador inválido: ${String(id)}`)
  }
  // Se devuelve un número (no un texto): no puede contener '/', '?' ni '..'
  return parsed
}
