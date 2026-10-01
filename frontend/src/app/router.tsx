import { createBrowserRouter } from 'react-router'
import { BackofficeLayout } from '../backoffice/BackofficeLayout'
import { RequirePermission } from '../backoffice/components/RequirePermission'
import { DashboardPage } from '../backoffice/pages/DashboardPage'
import { RolesPage } from '../backoffice/pages/roles/RolesPage'
import { UsersPage } from '../backoffice/pages/users/UsersPage'
import { Permissions } from '../shared/auth/permissions'
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
    children: [
      { index: true, element: <DashboardPage /> },
      {
        path: 'usuarios',
        element: (
          <RequirePermission permission={Permissions.USERS_READ}>
            <UsersPage />
          </RequirePermission>
        ),
      },
      {
        path: 'roles',
        element: (
          <RequirePermission permission={Permissions.ROLES_READ}>
            <RolesPage />
          </RequirePermission>
        ),
      },
    ],
  },
])
