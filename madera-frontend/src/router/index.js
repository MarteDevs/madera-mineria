import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ── RUTAS PÚBLICAS / AUTH ──────────────────────────────────────────────────
    {
      path: '/auth',
      component: () => import('@/layouts/AuthLayout.vue'),
      meta: { guestOnly: true },
      children: [
        {
          path: '/login',
          name: 'login',
          component: () => import('@/views/auth/LoginView.vue')
        },
        {
          path: '/register',
          name: 'register',
          component: () => import('@/views/auth/RegisterView.vue')
        }
      ]
    },
    // Redirección de raíz a dashboard
    {
      path: '/',
      redirect: '/dashboard'
    },
    // ── RUTAS PROTEGIDAS ────────────────────────────────────────────────────
    {
      path: '/',
      component: () => import('@/layouts/DashboardLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue')
        },
        {
          path: 'inventario',
          name: 'inventario',
          component: () => import('@/views/inventario/InventarioView.vue'),
          meta: { roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'] }
        },
        {
          path: 'inventario/nuevo',
          name: 'inventario-nuevo',
          component: () => import('@/views/inventario/InventarioFormView.vue'),
          meta: { roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'] }
        },
        {
          path: 'pedidos',
          name: 'pedidos',
          component: () => import('@/views/pedidos/PedidosView.vue')
        },
        {
          path: 'pedidos/nuevo',
          name: 'pedido-nuevo',
          component: () => import('@/views/pedidos/PedidoFormView.vue'),
          meta: { roles: ['ROLE_COMPRAS', 'ROLE_ADMIN'] }
        },
        {
          path: 'pedidos/:id',
          name: 'pedido-detalle',
          component: () => import('@/views/pedidos/PedidoDetailView.vue')
        },
        {
          path: 'entregas',
          name: 'entregas',
          component: () => import('@/views/entregas/EntregasView.vue'),
          meta: { roles: ['ROLE_ADMIN', 'ROLE_ALMACEN', 'ROLE_TRANSPORTE'] }
        },
        {
          path: 'entregas/:id',
          name: 'entrega-detalle',
          component: () => import('@/views/entregas/EntregaDetailView.vue'),
          meta: { roles: ['ROLE_ADMIN', 'ROLE_ALMACEN', 'ROLE_TRANSPORTE'] }
        },
        {
          path: 'notificaciones',
          name: 'notificaciones',
          component: () => import('@/views/notificaciones/NotificacionesView.vue'),
          meta: { roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'] }
        },
        {
          path: 'usuarios',
          name: 'usuarios',
          component: () => import('@/views/usuarios/UsuariosView.vue'),
          meta: { roles: ['ROLE_ADMIN'] }
        }
      ]
    },
    // Redirección por defecto
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

// Navigation Guards
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')
  const usuario = JSON.parse(localStorage.getItem('usuario') || 'null')
  const isAuthenticated = !!token

  // Rutas protegidas
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isAuthenticated) {
      return '/login'
    }
    
    // Verificar rol si está definido en alguna parte de la jerarquía de rutas
    const requiredRoles = to.matched.find(record => record.meta.roles)?.meta.roles
    if (requiredRoles && usuario) {
      if (!requiredRoles.includes(usuario.rol)) {
        return '/dashboard' // Redirige al dashboard si no tiene rol autorizado
      }
    }
  }

  // Rutas de invitado únicamente (Login, Register)
  if (to.matched.some(record => record.meta.guestOnly)) {
    if (isAuthenticated) {
      return '/dashboard'
    }
  }
})

export default router
