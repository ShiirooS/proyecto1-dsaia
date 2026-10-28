/** Solo se admiten rutas internas, para evitar open redirects vía `?redirect=`. */
export function getSafeRedirect(raw: string | null): string {
  return raw && raw.startsWith('/') && !raw.startsWith('//') ? raw : '/'
}
