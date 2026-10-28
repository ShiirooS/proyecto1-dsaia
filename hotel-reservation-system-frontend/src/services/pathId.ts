/**
 * Convierte un id en un segmento de URL seguro. Solo acepta enteros positivos, de modo que un valor
 * manipulado (p. ej. '../user/get-all' desde la ruta del navegador) nunca altera el endpoint llamado.
 * Hallazgo de SonarCloud (tssecurity:S7044 / S8476) sobre los servicios que interpolaban el id.
 */
export function toPathId(id: unknown): string {
  const value = typeof id === 'string' ? id.trim() : id
  const parsed = typeof value === 'number' ? value : Number(value)
  if (!Number.isSafeInteger(parsed) || parsed <= 0 || String(parsed) !== String(value)) {
    throw new Error(`Identificador inválido: ${String(id)}`)
  }
  return encodeURIComponent(String(parsed))
}
