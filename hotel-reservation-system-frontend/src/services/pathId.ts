/**
 * Convierte un id en un número seguro para usar como segmento de URL. Solo acepta enteros positivos, de modo
 * que un valor manipulado (p. ej. '../user/get-all' desde la ruta del navegador) nunca altera el endpoint llamado.
 * Hallazgo de SonarCloud (tssecurity:S7044 / S8476) sobre los servicios que interpolaban el id.
 */
export function toPathId(id: unknown): number {
  const text = String(id).trim()
  // parseInt siempre crea un número nuevo: no puede contener '/', '?' ni '..'
  const parsed = Number.parseInt(text, 10)
  if (!Number.isSafeInteger(parsed) || parsed <= 0 || String(parsed) !== text) {
    throw new Error(`Identificador inválido: ${text}`)
  }
  return parsed
}
