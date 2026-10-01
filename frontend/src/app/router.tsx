import { createBrowserRouter } from 'react-router'
import { BackofficeLayout } from '../backoffice/BackofficeLayout'
import { DashboardPage } from '../backoffice/pages/DashboardPage'
import { StorefrontLayout } from '../storefront/StorefrontLayout'
import { HomePage } from '../storefront/pages/HomePage'
import { LoginPage } from '../storefront/pages/LoginPage'
import { RegisterPage } from '../storefront/pages/RegisterPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <StorefrontLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'registro', element: <RegisterPage /> },
    ],
  },
  {
    path: '/admin',
    element: <BackofficeLayout />,
    children: [{ index: true, element: <DashboardPage /> }],
  },
])
