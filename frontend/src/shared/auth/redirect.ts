/**
 * Destino tras el login. Solo acepta rutas internas ("/algo"), para que un enlace
 * como /login?redirect=https://otro-sitio no saque al usuario de la tienda.
 */
export function safeRedirect(target: string | null, fallback = '/'): string {
  if (!target || !target.startsWith('/') || target.startsWith('//') || target.startsWith('/\\')) {
    return fallback
  }
  return target
}
