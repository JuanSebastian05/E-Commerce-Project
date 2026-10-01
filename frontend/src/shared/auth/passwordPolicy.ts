/**
 * Misma política que el backend (RN-02): de 8 caracteres a 72 bytes, con al menos una letra
 * y un número. Solo sirve para avisar antes de enviar; quien decide es el backend.
 */
export function passwordPolicyError(password: string): string | null {
  if ([...password].length < 8) {
    return 'Debe tener al menos 8 caracteres'
  }
  if (new TextEncoder().encode(password).length > 72) {
    return 'Es demasiado larga'
  }
  if (!/\p{L}/u.test(password) || !/\p{Nd}/u.test(password)) {
    return 'Debe contener al menos una letra y un número'
  }
  return null
}
