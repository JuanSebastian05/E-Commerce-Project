# Seguridad

Decisiones en [ADR-003](../architecture/decisions/ADR-003-autenticacion-y-autorizacion.md).

## Autenticación

- `POST /api/v1/auth/login` comprueba email y contraseña y devuelve un access token JWT (HS256, 15 minutos) en el cuerpo y un refresh token en la cookie `refresh_token`.
- El token lleva `iss`, `sub` (id del usuario), `iat`, `exp` y `permissions`. Se rechaza si la firma, el emisor o la expiración no son válidos.
- La clave de firma llega por `JWT_SECRET` (mínimo 32 caracteres). Sin ella la aplicación no arranca.
- Las contraseñas se guardan con BCrypt (coste 12). Política: mínimo 8 caracteres, máximo 72 bytes, al menos una letra y un número (RN-02).
- Credenciales incorrectas, email inexistente y usuario desactivado devuelven el mismo `401` con el mismo mensaje. Si el email no existe, igualmente se calcula un BCrypt, para que el tiempo de respuesta no revele qué emails están registrados.

## Sesión (refresh token)

- Valor aleatorio de 256 bits, válido 7 días. En la base de datos solo se guarda su SHA-256.
- Cookie `HttpOnly` (JavaScript no la puede leer), `Secure`, `SameSite=Strict` y `Path=/api/v1/auth`.
- **Rotación:** cada `POST /auth/refresh` revoca el token usado y emite otro de la misma familia (`family_id`).
- **Detección de robo:** si llega un token ya revocado, se revoca toda la familia; tanto el atacante como el usuario legítimo tienen que volver a iniciar sesión.
- La fila del token se bloquea (`SELECT ... FOR UPDATE`) durante la renovación, para que dos peticiones simultáneas con el mismo token no generen dos sesiones. Consecuencia: si dos pestañas renuevan a la vez con el mismo token, la segunda se trata como reutilización y la sesión se cierra.
- Al renovar se vuelven a leer los permisos y se comprueba que el usuario siga activo; si está desactivado, se revoca la sesión.
- `POST /auth/logout` revoca la familia del token de la cookie y borra la cookie.
- `REFRESH_COOKIE_SECURE=false` solo para entornos de prueba sin HTTPS distintos de localhost (los navegadores ya aceptan cookies `Secure` en http://localhost).

## Límite de intentos de login

- Máximo 5 intentos fallidos por combinación de email e IP en una ventana deslizante de 15 minutos. Al superarlo se responde `429` con `Retry-After`, aunque la contraseña sea correcta.
- Un login correcto reinicia el contador.
- La IP es la dirección remota de la conexión. Si en el futuro hay un proxy delante, habrá que configurar las cabeceras `X-Forwarded-For` de forma explícita.
- El estado está en memoria: solo es correcto con una instancia del backend (ver ADR-003).

## Autorización

- Por permisos, nunca por nombre de rol (RN-05): `@PreAuthorize("hasAuthority('users:read')")`.
- Los permisos del token son la unión de los de todos los roles del usuario en el momento del login.
- `/users/me` lee roles y permisos de la base de datos, no del token, para mostrar siempre el estado actual.

## Cambios de acceso

- Desactivar un usuario o cambiar sus roles publica `UserAccessChangedEvent`. El módulo Auth lo escucha y revoca todos sus refresh tokens dentro de la misma transacción.
- El access token que ya tenga sigue siendo válido hasta que caduque (15 minutos como máximo, ver ADR-003).
- Reglas de protección: nadie puede desactivarse a sí mismo ni quitarse el permiso `users:manage`, y siempre debe quedar al menos un usuario activo con `roles:manage` (RN-08).
- Riesgo conocido: si dos administradores se desactivan entre sí exactamente a la vez, la comprobación de RN-08 podría no detectarlo. Se acepta por ahora por ser muy improbable; la solución sería bloquear las filas de los administradores durante el cambio.
- Hasta que exista el módulo Audit, las acciones administrativas se registran en el log con los ids del actor y del usuario afectado, sin datos sensibles.

## Primer administrador

Al arrancar, si no existe ningún usuario con rol ADMIN y están definidas `ADMIN_EMAIL` y `ADMIN_PASSWORD`, se crea uno. Si ya existe, no se modifica nada.

## Otras medidas

- API stateless, sin sesiones ni cookies de sesión; CSRF desactivado por ese motivo.
- CORS limitado a `CORS_ALLOWED_ORIGINS`.
- Las respuestas de error nunca incluyen trazas ni mensajes internos; las respuestas de usuario nunca incluyen el hash de la contraseña.

## Pruebas de seguridad (`AuthApiIntegrationTest`)

- Acceso sin token, con token manipulado o con un texto que no es JWT → `401`.
- Token firmado con otra clave que declara permisos de ADMIN → `401`.
- Mismo `401` para contraseña incorrecta, email inexistente y usuario desactivado.
- El registro nunca devuelve la contraseña ni su hash, y el hash guardado es BCrypt.

## Pruebas de sesión (`SessionApiIntegrationTest`, `LoginRateLimiterTest`)

- Atributos de la cookie (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path`) y que el refresh token nunca va en el cuerpo.
- La rotación emite un token distinto y un access token válido.
- Reutilizar un token rotado revoca también el token más reciente de la sesión.
- Refresh sin cookie, con un token inventado, tras logout o con el usuario desactivado → `401`.
- Tras 5 fallos, el login responde `429` aunque la contraseña sea correcta; otros emails e IPs no se ven afectados y el bloqueo se levanta al salir de la ventana.

## Pruebas de administración (`UserAdministrationApiIntegrationTest`, `UserAdministrationServiceTest`)

- Sin token → `401`. CUSTOMER → `403` en el listado. SUPPORT puede listar pero no crear (`403`).
- Escalada vertical: un CUSTOMER que intenta asignarse ADMIN recibe `403`.
- Al desactivar a un usuario o cambiar sus roles, su refresh token deja de valer y sus nuevos permisos se aplican en el siguiente login.
- Un ADMIN no puede desactivarse ni quitarse `users:manage`.
- RN-08: no se puede desactivar al último usuario activo con `roles:manage`.

## Cambios en los permisos de un rol

- Llegan a los usuarios de ese rol en su siguiente renovación de sesión, como mucho 15 minutos después, porque el refresh vuelve a leer los permisos. No se revocan sesiones: un rol como CUSTOMER puede tener muchos usuarios.
- El rol ADMIN siempre conserva `users:manage` y `roles:manage`, y se sigue aplicando RN-08.

Pruebas en `RoleApiIntegrationTest`: SUPPORT no puede leer ni crear roles (`403`); un rol nuevo da sus permisos a sus usuarios; validaciones de nombre y de permisos; protección del rol ADMIN; no se pueden borrar roles de sistema ni roles con usuarios.

## Deuda técnica

- Los refresh tokens caducados o revocados no se borran todavía. Habrá que añadir una limpieza periódica cuando el volumen lo justifique.
