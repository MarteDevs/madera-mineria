import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        {
          path: '',
          name: 'login',
          component: () => import('@/views/auth/LoginView.vue')
        }
      ],
      meta: { guestOnly: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/DashboardLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue')
        },
        {
          path: 'inventario',
          name: 'inventario',
          component: () => import('@/views/inventario/InventarioView.vue')
        },
        {
          path: 'pedidos',
          name: 'pedidos',
          component: () => import('@/views/pedidos/PedidosView.vue')
        },
        {
          path: 'entregas',
          name: 'entregas',
          component: () => import('@/views/entregas/EntregasView.vue')
        }
      ]
    }
  ]
});

// Navigation Guards
router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    next('/login');
  } else if (to.meta.guestOnly && auth.isAuthenticated) {
    next('/');
  } else {
    next();
  }
});

export default router;
