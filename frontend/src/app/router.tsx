import { createBrowserRouter } from 'react-router'
import { BackofficeLayout } from '../backoffice/BackofficeLayout'
import { DashboardPage } from '../backoffice/pages/DashboardPage'
import { StorefrontLayout } from '../storefront/StorefrontLayout'
import { HomePage } from '../storefront/pages/HomePage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <StorefrontLayout />,
    children: [{ index: true, element: <HomePage /> }],
  },
  {
    path: '/admin',
    element: <BackofficeLayout />,
    children: [{ index: true, element: <DashboardPage /> }],
  },
])
