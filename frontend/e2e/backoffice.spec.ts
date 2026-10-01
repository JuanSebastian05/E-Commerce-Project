import { expect, test, type Page } from '@playwright/test'
import { adminCredentials, login, loginAsAdmin, PASSWORD, register, uniqueEmail, uniqueSuffix } from './helpers.ts'

const backofficeMenu = (page: Page) => page.getByRole('navigation', { name: 'Backoffice' })

/** Fila de datos del usuario (la fila de edición de roles que se abre debajo también menciona el email). */
const userRow = (page: Page, email: string) => page.getByRole('row', { name: email }).first()

test('un visitante que abre el Backoffice va al login y vuelve tras entrar', async ({ page }) => {
  test.skip(!adminCredentials.email, 'Define E2E_ADMIN_EMAIL y E2E_ADMIN_PASSWORD para esta prueba')

  await page.goto('/admin/usuarios?estado=activos')
  await expect(page).toHaveURL('/login?redirect=%2Fadmin%2Fusuarios%3Festado%3Dactivos')

  await login(page, adminCredentials.email!, adminCredentials.password!)

  await expect(page).toHaveURL('/admin/usuarios?estado=activos')
  await expect(page.getByRole('heading', { name: 'Usuarios' })).toBeVisible()
})

test('un cliente no entra al Backoffice', async ({ page }) => {
  await register(page, uniqueEmail())
  await expect(page.getByText('Hola, Ana')).toBeVisible()

  await page.goto('/admin/usuarios')

  await expect(page.getByRole('heading', { name: 'Sin acceso' })).toBeVisible()
  await expect(backofficeMenu(page)).toHaveCount(0)
})

test.describe('con el ADMIN', () => {
  test.skip(!adminCredentials.email || !adminCredentials.password, 'Define E2E_ADMIN_EMAIL y E2E_ADMIN_PASSWORD')

  test('crea un usuario de soporte, lo desactiva y lo reactiva', async ({ page }) => {
    const email = uniqueEmail()
    await loginAsAdmin(page, '/admin/usuarios')

    await page.getByRole('button', { name: 'Nuevo usuario' }).click()
    const form = page.getByRole('form', { name: 'Nuevo usuario' })
    await form.getByLabel('Nombre', { exact: true }).fill('Sofía')
    await form.getByLabel('Apellido').fill('Ruiz')
    await form.getByLabel('Email').fill(email)
    await form.getByLabel('Contraseña inicial').fill(PASSWORD)
    await form.getByLabel('SUPPORT').check()
    await form.getByRole('button', { name: 'Crear usuario' }).click()
    await expect(form).toHaveCount(0)

    await page.getByLabel('Email contiene').fill(email)
    await page.getByRole('button', { name: 'Buscar' }).click()
    const row = userRow(page, email)
    await expect(row).toContainText('Sofía Ruiz')
    await expect(row).toContainText('SUPPORT')
    await expect(row).toContainText('Activo')

    await row.getByRole('button', { name: 'Desactivar' }).click()
    await expect(row).toContainText('Desactivado')
    await row.getByRole('button', { name: 'Activar' }).click()
    await expect(row).toContainText('Activo')
  })

  test('cambia los roles de un cliente', async ({ page }) => {
    const email = uniqueEmail()
    await register(page, email)
    await page.getByRole('button', { name: 'Cerrar sesión' }).click()
    await loginAsAdmin(page, `/admin/usuarios?email=${encodeURIComponent(email)}`)

    const row = userRow(page, email)
    await expect(row).toContainText('CUSTOMER')
    await row.getByRole('button', { name: 'Cambiar roles' }).click()
    const editor = page.getByRole('group', { name: `Roles de ${email}` })
    await editor.getByLabel('WAREHOUSE').check()
    await page.getByRole('button', { name: 'Guardar roles' }).click()

    await expect(row).toContainText('CUSTOMER, WAREHOUSE')
  })

  test('el ADMIN no puede desactivarse a sí mismo', async ({ page }) => {
    await loginAsAdmin(page, `/admin/usuarios?email=${encodeURIComponent(adminCredentials.email!)}`)

    const row = userRow(page, adminCredentials.email!)
    await expect(row).toContainText('(tú)')
    await row.getByRole('button', { name: 'Desactivar' }).click()

    await expect(page.getByRole('alert')).toContainText('No puedes desactivar tu propia cuenta')
    await expect(row).toContainText('Activo')
  })

  test('crea un rol, cambia sus permisos y lo borra', async ({ page }) => {
    const roleName = `AUDITOR_${uniqueSuffix().toUpperCase()}`
    await loginAsAdmin(page, '/admin/roles')

    await page.getByRole('button', { name: 'Nuevo rol' }).click()
    const form = page.getByRole('form', { name: 'Nuevo rol' })
    await form.getByLabel('Nombre del rol').fill(roleName)
    await form.getByLabel('Descripción').fill('Solo consulta usuarios')
    await form.getByLabel('users:read').check()
    await form.getByRole('button', { name: 'Crear rol' }).click()
    await expect(form).toHaveCount(0)

    const card = page.getByRole('article', { name: `Rol ${roleName}` })
    const granted = card.getByRole('list', { name: `Permisos de ${roleName}` })
    await expect(granted.getByRole('listitem')).toHaveText(['users:read'])

    await card.getByRole('button', { name: 'Editar permisos' }).click()
    await card.getByLabel('roles:read').check()
    await card.getByRole('button', { name: 'Guardar permisos' }).click()
    await expect(granted.getByRole('listitem')).toHaveText(['roles:read', 'users:read'])

    await card.getByRole('button', { name: 'Borrar' }).click()
    await card.getByRole('button', { name: 'Sí, borrar' }).click()
    await expect(card).toHaveCount(0)
  })

  test('un nombre de rol repetido muestra el conflicto', async ({ page }) => {
    await loginAsAdmin(page, '/admin/roles')

    await page.getByRole('button', { name: 'Nuevo rol' }).click()
    const form = page.getByRole('form', { name: 'Nuevo rol' })
    await form.getByLabel('Nombre del rol').fill('support')
    await form.getByRole('button', { name: 'Crear rol' }).click()

    await expect(form.getByRole('alert')).toHaveText('Ya existe un rol con ese nombre')
  })

  test('el rol ADMIN no puede perder la administración y los roles de sistema no se borran', async ({ page }) => {
    await loginAsAdmin(page, '/admin/roles')

    const admin = page.getByRole('article', { name: 'Rol ADMIN' })
    await admin.getByRole('button', { name: 'Editar permisos' }).click()
    await expect(admin.getByLabel('users:manage')).toBeDisabled()
    await expect(admin.getByLabel('roles:manage')).toBeDisabled()
    await expect(admin.getByLabel('users:read')).toBeEnabled()
    await expect(admin.getByRole('button', { name: 'Borrar' })).toHaveCount(0)
  })

  test('un usuario de soporte ve los usuarios pero no puede gestionarlos ni ver los roles', async ({ page }) => {
    const email = uniqueEmail()
    await loginAsAdmin(page, '/admin/usuarios')
    await page.getByRole('button', { name: 'Nuevo usuario' }).click()
    const form = page.getByRole('form', { name: 'Nuevo usuario' })
    await form.getByLabel('Nombre', { exact: true }).fill('Luis')
    await form.getByLabel('Apellido').fill('Soporte')
    await form.getByLabel('Email').fill(email)
    await form.getByLabel('Contraseña inicial').fill(PASSWORD)
    await form.getByLabel('SUPPORT').check()
    await form.getByRole('button', { name: 'Crear usuario' }).click()
    await expect(form).toHaveCount(0)
    await page.getByRole('button', { name: 'Cerrar sesión' }).click()

    await page.goto('/login?redirect=/admin/usuarios')
    await login(page, email, PASSWORD)

    await expect(page.getByRole('heading', { name: 'Usuarios' })).toBeVisible()
    await expect(backofficeMenu(page).getByRole('link', { name: 'Usuarios' })).toBeVisible()
    await expect(backofficeMenu(page).getByRole('link', { name: 'Roles' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Nuevo usuario' })).toHaveCount(0)
    await expect(page.getByRole('columnheader', { name: 'Acciones' })).toHaveCount(0)

    await page.goto('/admin/roles')
    await expect(page.getByRole('heading', { name: 'Sin acceso' })).toBeVisible()
  })
})
