import { expect, type Page } from '@playwright/test'

export const PASSWORD = 'Clave1234'

export const adminCredentials = {
  email: process.env.E2E_ADMIN_EMAIL,
  password: process.env.E2E_ADMIN_PASSWORD,
}

export function uniqueEmail() {
  return `e2e-${uniqueSuffix()}@example.com`
}

export function uniqueSuffix() {
  return `${Date.now()}${Math.random().toString(36).slice(2, 8)}`
}

/** Registra un cliente desde la tienda; queda con la sesión iniciada. */
export async function register(page: Page, email: string, password = PASSWORD) {
  await page.goto('/registro')
  await page.getByLabel('Nombre').fill('Ana')
  await page.getByLabel('Apellido').fill('Pérez')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Contraseña').fill(password)
  await page.getByRole('button', { name: 'Crear cuenta' }).click()
}

/** Rellena el formulario de login de la página actual. */
export async function login(page: Page, email: string, password: string) {
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Contraseña').fill(password)
  await page.getByRole('button', { name: 'Entrar' }).click()
}

export async function loginAsAdmin(page: Page, redirect = '/admin') {
  await page.goto(`/login?redirect=${encodeURIComponent(redirect)}`)
  await login(page, adminCredentials.email!, adminCredentials.password!)
  await expect(page).toHaveURL(redirect)
}

export async function logoutFromStore(page: Page) {
  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()
}
