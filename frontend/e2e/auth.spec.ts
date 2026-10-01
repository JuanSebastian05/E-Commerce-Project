import { expect, test, type Page, type Route } from '@playwright/test'

const PASSWORD = 'Clave1234'

function uniqueEmail() {
  return `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`
}

async function register(page: Page, email: string, password = PASSWORD) {
  await page.goto('/registro')
  await page.getByLabel('Nombre').fill('Ana')
  await page.getByLabel('Apellido').fill('Pérez')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Contraseña').fill(password)
  await page.getByRole('button', { name: 'Crear cuenta' }).click()
}

async function login(page: Page, email: string, password: string) {
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Contraseña').fill(password)
  await page.getByRole('button', { name: 'Entrar' }).click()
}

test('un visitante se registra, la sesión sobrevive a recargar y puede cerrarla', async ({ page }) => {
  await register(page, uniqueEmail())

  await expect(page).toHaveURL('/')
  await expect(page.getByText('Hola, Ana')).toBeVisible()
  // Un cliente no tiene acceso al Backoffice.
  await expect(page.getByRole('link', { name: 'Backoffice' })).toHaveCount(0)

  // El access token solo está en memoria: tras recargar, la sesión vuelve con la cookie.
  await page.reload()
  await expect(page.getByText('Hola, Ana')).toBeVisible()

  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()

  // La cookie quedó revocada: al recargar no hay sesión.
  await page.reload()
  await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()
  await expect(page.getByText('Hola, Ana')).toHaveCount(0)
})

test('dos pestañas que cargan a la vez conservan la sesión', async ({ page, context }) => {
  await register(page, uniqueEmail())
  await expect(page.getByText('Hola, Ana')).toBeVisible()

  // Ambas pestañas renuevan la sesión al cargar. Si enviaran la misma cookie a la vez, el
  // backend lo vería como reutilización del refresh token y cerraría todas las sesiones.
  // Para forzar la coincidencia, cada renovación espera hasta 1 s a la de la otra pestaña.
  await context.route('**/api/v1/auth/refresh', holdUntilBothArrive())
  const otherPage = await context.newPage()
  await Promise.all([page.reload(), otherPage.goto('/')])

  await expect(page.getByText('Hola, Ana')).toBeVisible()
  await expect(otherPage.getByText('Hola, Ana')).toBeVisible()
})

test('si el access token caduca, se renueva y la petición se repite', async ({ page }) => {
  await register(page, uniqueEmail())
  await expect(page.getByText('Hola, Ana')).toBeVisible()

  // La primera llamada a /users/me responde 401, como haría con un token caducado.
  let rejected = false
  await page.route('**/api/v1/users/me', async (route) => {
    if (!rejected) {
      rejected = true
      await route.fulfill({ status: 401 })
    } else {
      await route.continue()
    }
  })
  const refreshes: string[] = []
  page.on('request', (request) => {
    if (request.url().endsWith('/api/v1/auth/refresh')) refreshes.push(request.url())
  })

  await page.reload()

  await expect(page.getByText('Hola, Ana')).toBeVisible()
  expect(rejected).toBe(true)
  // Una renovación al cargar la página y otra tras el 401.
  expect(refreshes).toHaveLength(2)
})

function holdUntilBothArrive() {
  let waiting: Route[] = []
  const release = () => {
    const routes = waiting
    waiting = []
    routes.forEach((route) => void route.continue())
  }
  return (route: Route) => {
    waiting.push(route)
    if (waiting.length === 2) {
      release()
    } else {
      setTimeout(release, 1000)
    }
  }
}

test('un cliente registrado inicia sesión y vuelve a la página que pidió', async ({ page }) => {
  const email = uniqueEmail()
  await register(page, email)
  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()

  await page.goto('/login?redirect=/admin')
  await login(page, email, PASSWORD)

  await expect(page).toHaveURL('/admin')
})

test('el login no redirige fuera de la tienda', async ({ page }) => {
  const email = uniqueEmail()
  await register(page, email)
  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()

  await page.goto('/login?redirect=//example.org')
  await login(page, email, PASSWORD)

  await expect(page).toHaveURL('/')
})

test('con una contraseña incorrecta se muestra el error genérico', async ({ page }) => {
  await page.goto('/login')
  await login(page, uniqueEmail(), 'Incorrecta123')

  await expect(page.getByRole('alert')).toHaveText('Email o contraseña incorrectos')
  await expect(page).toHaveURL(/\/login$/)
})

test('el registro avisa de una contraseña débil sin llamar a la API', async ({ page }) => {
  let registerCalls = 0
  page.on('request', (request) => {
    if (request.url().endsWith('/api/v1/auth/register')) registerCalls++
  })

  await register(page, uniqueEmail(), 'solotexto')

  await expect(page.getByText('Debe contener al menos una letra y un número')).toBeVisible()
  expect(registerCalls).toBe(0)
})

test('un email ya registrado muestra el conflicto', async ({ page }) => {
  const email = uniqueEmail()
  await register(page, email)
  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()

  await register(page, email)

  await expect(page.getByRole('alert')).toHaveText('Ya existe una cuenta con ese email')
})

test('el ADMIN ve el acceso al Backoffice', async ({ page }) => {
  const email = process.env.E2E_ADMIN_EMAIL
  const password = process.env.E2E_ADMIN_PASSWORD
  test.skip(!email || !password, 'Define E2E_ADMIN_EMAIL y E2E_ADMIN_PASSWORD para esta prueba')

  await page.goto('/login')
  await login(page, email!, password!)

  await expect(page.getByRole('link', { name: 'Backoffice' })).toBeVisible()
})
