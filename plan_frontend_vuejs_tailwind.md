# Plan Técnico — Frontend Vue.js + Tailwind CSS
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Tecnología:** Vue.js 3 + Tailwind CSS + Vite  
> **Consume:** api-gateway en `http://localhost:8080`  
> **Puerto:** `5173` (desarrollo) / `80` (Docker)  
> **Autenticación:** JWT almacenado en localStorage

---

## Pantallas a implementar

| Pantalla | Ruta | Rol que la usa |
|---|---|---|
| Login | `/login` | Todos |
| Registro | `/register` | Admin |
| Dashboard | `/dashboard` | Todos (según rol) |
| Inventario | `/inventario` | Admin, Almacén |
| Nuevo ítem inventario | `/inventario/nuevo` | Admin, Almacén |
| Pedidos | `/pedidos` | Todos |
| Nuevo pedido | `/pedidos/nuevo` | Compras |
| Detalle pedido | `/pedidos/:id` | Todos |
| Entregas | `/entregas` | Almacén, Transporte |
| Detalle entrega | `/entregas/:id` | Almacén, Transporte |
| Notificaciones | `/notificaciones` | Almacén |
| Usuarios | `/usuarios` | Admin |

---

## Tecnologías del frontend

| Librería | Para qué |
|---|---|
| **Vue.js 3** | Framework principal con Composition API |
| **Vite** | Bundler rápido para desarrollo |
| **Tailwind CSS** | Estilos profesionales sin CSS personalizado |
| **Vue Router 4** | Navegación entre páginas y guards de autenticación |
| **Pinia** | Manejo de estado global (token, usuario, rol) |
| **Axios** | Llamadas HTTP al api-gateway |
| **Chart.js + vue-chartjs** | Gráficas en el dashboard |
| **Heroicons** | Iconos SVG profesionales |

---

## Estructura de archivos

```
madera-frontend/
├── index.html
├── vite.config.js
├── tailwind.config.js
├── package.json
│
└── src/
    ├── main.js                        ← punto de entrada
    ├── App.vue                        ← componente raíz
    │
    ├── router/
    │   └── index.js                   ← rutas + guards JWT
    │
    ├── stores/
    │   ├── auth.js                    ← token, usuario, rol (Pinia)
    │   ├── inventario.js              ← estado del inventario
    │   ├── pedidos.js                 ← estado de pedidos
    │   ├── entregas.js                ← estado de entregas
    │   └── notificaciones.js          ← notificaciones pendientes
    │
    ├── services/
    │   ├── api.js                     ← instancia de Axios + interceptors
    │   ├── authService.js             ← login, register
    │   ├── inventarioService.js       ← CRUD inventario
    │   ├── pedidosService.js          ← CRUD pedidos
    │   ├── entregasService.js         ← gestión entregas
    │   └── notificacionesService.js   ← notificaciones
    │
    ├── layouts/
    │   ├── AuthLayout.vue             ← layout para login/register
    │   └── DashboardLayout.vue        ← layout con sidebar y navbar
    │
    ├── views/
    │   ├── auth/
    │   │   ├── LoginView.vue
    │   │   └── RegisterView.vue
    │   ├── dashboard/
    │   │   └── DashboardView.vue
    │   ├── inventario/
    │   │   ├── InventarioView.vue
    │   │   └── InventarioFormView.vue
    │   ├── pedidos/
    │   │   ├── PedidosView.vue
    │   │   ├── PedidoFormView.vue
    │   │   └── PedidoDetailView.vue
    │   ├── entregas/
    │   │   ├── EntregasView.vue
    │   │   └── EntregaDetailView.vue
    │   ├── notificaciones/
    │   │   └── NotificacionesView.vue
    │   └── usuarios/
    │       └── UsuariosView.vue
    │
    └── components/
        ├── common/
        │   ├── Navbar.vue
        │   ├── Sidebar.vue
        │   ├── LoadingSpinner.vue
        │   ├── AlertMessage.vue
        │   ├── ConfirmModal.vue
        │   └── BadgeEstado.vue        ← badge de colores por estado
        ├── inventario/
        │   └── MaderaCard.vue
        ├── pedidos/
        │   ├── PedidoCard.vue
        │   └── PedidoTimeline.vue     ← historial de estados
        └── dashboard/
            ├── StatsCard.vue
            └── GraficaPedidos.vue
```

---

## Paso 1 — Crear el proyecto

```bash
# Crear proyecto Vue con Vite
npm create vue@latest madera-frontend

# Opciones a seleccionar:
# ✅ Add TypeScript? → No
# ✅ Add JSX Support? → No
# ✅ Add Vue Router? → Yes
# ✅ Add Pinia? → Yes
# ✅ Add Vitest? → No
# ✅ Add ESLint? → Yes

# Entrar a la carpeta
cd madera-frontend

# Instalar dependencias base
npm install

# Instalar Tailwind CSS
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# Instalar librerías adicionales
npm install axios
npm install chart.js vue-chartjs
npm install @heroicons/vue
```

---

## Paso 2 — Configurar Tailwind CSS

### `tailwind.config.js`

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Paleta de colores del sistema de madera
        primary: {
          50:  '#fdf8f0',
          100: '#faefd8',
          500: '#c8811a',
          600: '#a86a14',
          700: '#8a5510',
          900: '#5c3809',
        },
        madera: {
          claro:  '#DEB887',
          medio:  '#A0522D',
          oscuro: '#5C3317',
        }
      }
    },
  },
  plugins: [],
}
```

### `src/style.css`

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer components {
  /* Botones reutilizables */
  .btn-primary {
    @apply bg-primary-600 hover:bg-primary-700 text-white
           font-medium px-4 py-2 rounded-lg transition-colors
           duration-200 disabled:opacity-50 disabled:cursor-not-allowed;
  }

  .btn-secondary {
    @apply bg-gray-100 hover:bg-gray-200 text-gray-700
           font-medium px-4 py-2 rounded-lg transition-colors duration-200;
  }

  .btn-danger {
    @apply bg-red-600 hover:bg-red-700 text-white
           font-medium px-4 py-2 rounded-lg transition-colors duration-200;
  }

  /* Inputs */
  .input-field {
    @apply w-full border border-gray-300 rounded-lg px-3 py-2
           focus:outline-none focus:ring-2 focus:ring-primary-500
           focus:border-transparent text-sm;
  }

  /* Cards */
  .card {
    @apply bg-white rounded-xl shadow-sm border border-gray-100 p-6;
  }

  /* Tabla */
  .table-header {
    @apply px-6 py-3 text-left text-xs font-medium
           text-gray-500 uppercase tracking-wider;
  }

  .table-cell {
    @apply px-6 py-4 whitespace-nowrap text-sm text-gray-900;
  }
}
```

---

## Paso 3 — Configurar Axios e interceptors

### `src/services/api.js`

```javascript
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ── INTERCEPTOR DE REQUEST — agregar token JWT ────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── INTERCEPTOR DE RESPONSE — manejar errores globales ───────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expirado o inválido — cerrar sesión
      localStorage.removeItem('token')
      localStorage.removeItem('usuario')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default api
```

---

## Paso 4 — Configurar Pinia (stores)

### `src/stores/auth.js`

```javascript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'

export const useAuthStore = defineStore('auth', () => {

  const token   = ref(localStorage.getItem('token') || null)
  const usuario = ref(JSON.parse(localStorage.getItem('usuario') || 'null'))

  const isAuthenticated = computed(() => !!token.value)
  const rol             = computed(() => usuario.value?.rol || '')
  const nombreCompleto  = computed(() =>
    usuario.value ? `${usuario.value.nombre}` : '')

  // Helpers de rol
  const esAdmin     = computed(() => rol.value === 'ROLE_ADMIN')
  const esAlmacen   = computed(() => rol.value === 'ROLE_ALMACEN')
  const esCompras   = computed(() => rol.value === 'ROLE_COMPRAS')
  const esTransporte= computed(() => rol.value === 'ROLE_TRANSPORTE')

  async function login(email, password) {
    const { data } = await api.post('/api/auth/login', { email, password })
    token.value   = data.token
    usuario.value = {
      email:  data.email,
      nombre: data.nombre,
      rol:    data.rol
    }
    localStorage.setItem('token',   data.token)
    localStorage.setItem('usuario', JSON.stringify(usuario.value))
    return data
  }

  function logout() {
    token.value   = null
    usuario.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
  }

  return {
    token, usuario, isAuthenticated,
    rol, nombreCompleto,
    esAdmin, esAlmacen, esCompras, esTransporte,
    login, logout
  }
})
```

### `src/stores/pedidos.js`

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export const usePedidosStore = defineStore('pedidos', () => {

  const pedidos  = ref([])
  const loading  = ref(false)
  const error    = ref(null)

  async function fetchPedidos() {
    loading.value = true
    error.value   = null
    try {
      const { data } = await api.get('/api/pedidos')
      pedidos.value  = data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando pedidos'
    } finally {
      loading.value = false
    }
  }

  async function fetchPorMina(mina) {
    const { data } = await api.get(`/api/pedidos/mina/${mina}`)
    pedidos.value  = data
  }

  async function crearPedido(pedidoData) {
    const { data } = await api.post('/api/pedidos', pedidoData)
    pedidos.value.unshift(data)
    return data
  }

  async function aprobarPedido(id, aprobadoPor) {
    const { data } = await api.put(
      `/api/pedidos/${id}/aprobar?aprobadoPor=${aprobadoPor}`)
    actualizarEnLista(data)
    return data
  }

  async function rechazarPedido(id, motivo) {
    const { data } = await api.put(
      `/api/pedidos/${id}/rechazar?motivo=${motivo}`)
    actualizarEnLista(data)
    return data
  }

  function actualizarEnLista(pedidoActualizado) {
    const idx = pedidos.value.findIndex(p => p.id === pedidoActualizado.id)
    if (idx !== -1) pedidos.value[idx] = pedidoActualizado
  }

  return { pedidos, loading, error,
           fetchPedidos, fetchPorMina,
           crearPedido, aprobarPedido, rechazarPedido }
})
```

---

## Paso 5 — Vue Router con guards

### `src/router/index.js`

```javascript
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ── RUTAS PÚBLICAS ──────────────────────────────────────────────────────
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { public: true }
    },

    // ── RUTAS PROTEGIDAS ────────────────────────────────────────────────────
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/dashboard/DashboardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/inventario',
      name: 'inventario',
      component: () => import('@/views/inventario/InventarioView.vue'),
      meta: { requiresAuth: true, roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'] }
    },
    {
      path: '/inventario/nuevo',
      name: 'inventario-nuevo',
      component: () => import('@/views/inventario/InventarioFormView.vue'),
      meta: { requiresAuth: true, roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'] }
    },
    {
      path: '/pedidos',
      name: 'pedidos',
      component: () => import('@/views/pedidos/PedidosView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/pedidos/nuevo',
      name: 'pedido-nuevo',
      component: () => import('@/views/pedidos/PedidoFormView.vue'),
      meta: { requiresAuth: true, roles: ['ROLE_COMPRAS', 'ROLE_ADMIN'] }
    },
    {
      path: '/pedidos/:id',
      name: 'pedido-detalle',
      component: () => import('@/views/pedidos/PedidoDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/entregas',
      name: 'entregas',
      component: () => import('@/views/entregas/EntregasView.vue'),
      meta: { requiresAuth: true,
              roles: ['ROLE_ADMIN', 'ROLE_ALMACEN', 'ROLE_TRANSPORTE'] }
    },
    {
      path: '/entregas/:id',
      name: 'entrega-detalle',
      component: () => import('@/views/entregas/EntregaDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/notificaciones',
      name: 'notificaciones',
      component: () => import('@/views/notificaciones/NotificacionesView.vue'),
      meta: { requiresAuth: true,
              roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'] }
    },
    {
      path: '/usuarios',
      name: 'usuarios',
      component: () => import('@/views/usuarios/UsuariosView.vue'),
      meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] }
    }
  ]
})

// ── GUARD GLOBAL ─────────────────────────────────────────────────────────────
router.beforeEach((to, from, next) => {
  const token   = localStorage.getItem('token')
  const usuario = JSON.parse(localStorage.getItem('usuario') || 'null')

  // Ruta pública — dejar pasar
  if (to.meta.public) {
    if (token) return next('/dashboard')  // ya logueado
    return next()
  }

  // Ruta protegida sin token — ir a login
  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }

  // Verificar rol si la ruta lo requiere
  if (to.meta.roles && usuario) {
    if (!to.meta.roles.includes(usuario.rol)) {
      return next('/dashboard')  // no tiene permiso
    }
  }

  next()
})

export default router
```

---

## Paso 6 — Layouts

### `src/layouts/DashboardLayout.vue`

```vue
<template>
  <div class="flex h-screen bg-gray-50">

    <!-- Sidebar -->
    <Sidebar />

    <!-- Contenido principal -->
    <div class="flex-1 flex flex-col overflow-hidden">

      <!-- Navbar superior -->
      <Navbar />

      <!-- Página actual -->
      <main class="flex-1 overflow-y-auto p-6">
        <router-view />
      </main>

    </div>
  </div>
</template>

<script setup>
import Sidebar from '@/components/common/Sidebar.vue'
import Navbar  from '@/components/common/Navbar.vue'
</script>
```

### `src/components/common/Sidebar.vue`

```vue
<template>
  <aside class="w-64 bg-stone-900 text-white flex flex-col">

    <!-- Logo -->
    <div class="p-6 border-b border-stone-700">
      <div class="flex items-center gap-3">
        <span class="text-2xl">🪵</span>
        <div>
          <h1 class="font-bold text-sm">Madera Minería</h1>
          <p class="text-xs text-stone-400">Sistema de Gestión</p>
        </div>
      </div>
    </div>

    <!-- Usuario actual -->
    <div class="p-4 border-b border-stone-700">
      <p class="text-xs text-stone-400">Conectado como</p>
      <p class="font-medium text-sm mt-1">{{ authStore.nombreCompleto }}</p>
      <span class="text-xs bg-amber-700 px-2 py-0.5 rounded-full mt-1 inline-block">
        {{ rolLegible }}
      </span>
    </div>

    <!-- Navegación -->
    <nav class="flex-1 p-4 space-y-1">

      <RouterLink to="/dashboard" class="nav-link">
        <span>📊</span> Dashboard
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen"
        to="/inventario" class="nav-link">
        <span>📦</span> Inventario
      </RouterLink>

      <RouterLink to="/pedidos" class="nav-link">
        <span>📋</span> Pedidos
      </RouterLink>

      <RouterLink
        v-if="!authStore.esCompras"
        to="/entregas" class="nav-link">
        <span>🚛</span> Entregas
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen"
        to="/notificaciones" class="nav-link">
        <span>🔔</span> Notificaciones
        <span v-if="pendientes > 0"
              class="ml-auto bg-red-500 text-xs px-2 py-0.5 rounded-full">
          {{ pendientes }}
        </span>
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin"
        to="/usuarios" class="nav-link">
        <span>👥</span> Usuarios
      </RouterLink>

    </nav>

    <!-- Botón cerrar sesión -->
    <div class="p-4 border-t border-stone-700">
      <button @click="logout"
              class="w-full text-left text-sm text-stone-400
                     hover:text-white transition-colors flex items-center gap-2">
        <span>🚪</span> Cerrar sesión
      </button>
    </div>

  </aside>
</template>

<script setup>
import { computed }       from 'vue'
import { useRouter }      from 'vue-router'
import { useAuthStore }   from '@/stores/auth'
import { useNotificacionesStore } from '@/stores/notificaciones'

const authStore   = useAuthStore()
const notifStore  = useNotificacionesStore()
const router      = useRouter()

const pendientes  = computed(() => notifStore.pendientes)

const rolLegible  = computed(() => {
  const mapa = {
    ROLE_ADMIN:      'Administrador',
    ROLE_ALMACEN:    'Almacén',
    ROLE_COMPRAS:    'Compras',
    ROLE_TRANSPORTE: 'Transporte'
  }
  return mapa[authStore.rol] || authStore.rol
})

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.nav-link {
  @apply flex items-center gap-3 px-3 py-2 rounded-lg text-sm
         text-stone-300 hover:bg-stone-700 hover:text-white
         transition-colors duration-150;
}
.router-link-active {
  @apply bg-amber-700 text-white;
}
</style>
```

---

## Paso 7 — Vistas principales

### `src/views/auth/LoginView.vue`

```vue
<template>
  <div class="min-h-screen bg-gradient-to-br from-stone-800 to-stone-900
              flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-md">

      <!-- Logo -->
      <div class="text-center mb-8">
        <span class="text-5xl">🪵</span>
        <h1 class="text-2xl font-bold text-gray-900 mt-3">Madera Minería</h1>
        <p class="text-gray-500 text-sm mt-1">Sistema de Gestión de Pedidos</p>
      </div>

      <!-- Formulario -->
      <form @submit.prevent="handleLogin" class="space-y-4">

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Email
          </label>
          <input v-model="form.email"
                 type="email"
                 placeholder="tu@email.com"
                 class="input-field"
                 required />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Contraseña
          </label>
          <input v-model="form.password"
                 type="password"
                 placeholder="••••••••"
                 class="input-field"
                 required />
        </div>

        <!-- Error -->
        <div v-if="error"
             class="bg-red-50 border border-red-200 text-red-700
                    rounded-lg px-4 py-3 text-sm">
          {{ error }}
        </div>

        <button type="submit"
                :disabled="loading"
                class="btn-primary w-full py-3">
          <span v-if="loading">Iniciando sesión...</span>
          <span v-else>Iniciar sesión</span>
        </button>

      </form>

    </div>
  </div>
</template>

<script setup>
import { ref }          from 'vue'
import { useRouter }    from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router    = useRouter()

const form    = ref({ email: '', password: '' })
const loading = ref(false)
const error   = ref(null)

async function handleLogin() {
  loading.value = true
  error.value   = null
  try {
    await authStore.login(form.value.email, form.value.password)
    router.push('/dashboard')
  } catch (e) {
    error.value = e.response?.data?.mensaje || 'Email o contraseña incorrectos'
  } finally {
    loading.value = false
  }
}
</script>
```

---

### `src/views/dashboard/DashboardView.vue`

```vue
<template>
  <div>
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Dashboard</h1>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">

      <StatsCard titulo="Pedidos Pendientes"
                 :valor="stats.pendientes"
                 icono="📋"
                 color="amber" />

      <StatsCard titulo="En Tránsito"
                 :valor="stats.enTransito"
                 icono="🚛"
                 color="blue" />

      <StatsCard titulo="Entregados Hoy"
                 :valor="stats.entregados"
                 icono="✅"
                 color="green" />

      <StatsCard titulo="Notificaciones"
                 :valor="stats.notificaciones"
                 icono="🔔"
                 color="red" />
    </div>

    <!-- Gráfica + Pedidos recientes -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

      <!-- Gráfica de pedidos por estado -->
      <div class="card">
        <h2 class="font-semibold text-gray-800 mb-4">Pedidos por estado</h2>
        <GraficaPedidos :datos="datoGrafica" />
      </div>

      <!-- Pedidos recientes -->
      <div class="card">
        <h2 class="font-semibold text-gray-800 mb-4">Pedidos recientes</h2>
        <div class="space-y-3">
          <div v-for="pedido in pedidosRecientes"
               :key="pedido.id"
               class="flex items-center justify-between
                      py-2 border-b border-gray-50 last:border-0">
            <div>
              <p class="text-sm font-medium text-gray-900">
                {{ pedido.tipoMadera }} — {{ pedido.mina }}
              </p>
              <p class="text-xs text-gray-500">
                {{ pedido.cantidadSolicitada }} unidades
              </p>
            </div>
            <BadgeEstado :estado="pedido.estado" />
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import StatsCard     from '@/components/dashboard/StatsCard.vue'
import GraficaPedidos from '@/components/dashboard/GraficaPedidos.vue'
import BadgeEstado   from '@/components/common/BadgeEstado.vue'
import api           from '@/services/api'

const stats = ref({
  pendientes: 0, enTransito: 0,
  entregados: 0, notificaciones: 0
})
const pedidosRecientes = ref([])
const datoGrafica      = ref({})

onMounted(async () => {
  const [pedidos, entregas, notif] = await Promise.all([
    api.get('/api/pedidos'),
    api.get('/api/entregas'),
    api.get('/api/notificaciones/pendientes/count')
  ])

  const p = pedidos.data
  stats.value.pendientes     = p.filter(x => x.estado === 'PENDIENTE').length
  stats.value.entregados     = p.filter(x => x.estado === 'ENTREGADO').length
  stats.value.enTransito     = entregas.data
    .filter(x => x.estado === 'EN_RUTA').length
  stats.value.notificaciones = notif.data

  pedidosRecientes.value = p.slice(0, 5)

  // Datos para la gráfica
  const conteo = p.reduce((acc, p) => {
    acc[p.estado] = (acc[p.estado] || 0) + 1
    return acc
  }, {})
  datoGrafica.value = conteo
})
</script>
```

---

### `src/views/pedidos/PedidosView.vue`

```vue
<template>
  <div>

    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Pedidos</h1>
      <RouterLink v-if="authStore.esCompras || authStore.esAdmin"
                  to="/pedidos/nuevo"
                  class="btn-primary">
        + Nuevo Pedido
      </RouterLink>
    </div>

    <!-- Filtros -->
    <div class="card mb-6">
      <div class="flex gap-4 flex-wrap">
        <select v-model="filtroEstado" class="input-field w-auto">
          <option value="">Todos los estados</option>
          <option value="PENDIENTE">Pendiente</option>
          <option value="APROBADO">Aprobado</option>
          <option value="EN_PREPARACION">En preparación</option>
          <option value="DESPACHADO">Despachado</option>
          <option value="ENTREGADO">Entregado</option>
          <option value="RECHAZADO">Rechazado</option>
        </select>

        <input v-model="filtroMina"
               placeholder="Filtrar por mina..."
               class="input-field w-48" />
      </div>
    </div>

    <!-- Loading -->
    <LoadingSpinner v-if="loading" />

    <!-- Tabla de pedidos -->
    <div v-else class="card p-0 overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="table-header">ID</th>
            <th class="table-header">Tipo Madera</th>
            <th class="table-header">Cantidad</th>
            <th class="table-header">Mina</th>
            <th class="table-header">Solicitado por</th>
            <th class="table-header">Estado</th>
            <th class="table-header">Fecha</th>
            <th class="table-header">Acciones</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="pedido in pedidosFiltrados"
              :key="pedido.id"
              class="hover:bg-gray-50 transition-colors">
            <td class="table-cell font-medium">#{{ pedido.id }}</td>
            <td class="table-cell capitalize">{{ pedido.tipoMadera }}</td>
            <td class="table-cell">{{ pedido.cantidadSolicitada }} {{ pedido.unidad }}</td>
            <td class="table-cell">{{ pedido.mina }}</td>
            <td class="table-cell">{{ pedido.solicitadoPor }}</td>
            <td class="table-cell">
              <BadgeEstado :estado="pedido.estado" />
            </td>
            <td class="table-cell text-gray-500 text-xs">
              {{ formatFecha(pedido.fechaPedido) }}
            </td>
            <td class="table-cell">
              <div class="flex gap-2">
                <RouterLink :to="`/pedidos/${pedido.id}`"
                            class="text-blue-600 hover:underline text-xs">
                  Ver
                </RouterLink>
                <button
                  v-if="authStore.esAlmacen && pedido.estado === 'PENDIENTE'"
                  @click="aprobar(pedido.id)"
                  class="text-green-600 hover:underline text-xs">
                  Aprobar
                </button>
                <button
                  v-if="authStore.esAlmacen && pedido.estado === 'PENDIENTE'"
                  @click="rechazar(pedido.id)"
                  class="text-red-600 hover:underline text-xs">
                  Rechazar
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="pedidosFiltrados.length === 0"
           class="text-center py-12 text-gray-400">
        No hay pedidos para mostrar
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore }   from '@/stores/auth'
import { usePedidosStore } from '@/stores/pedidos'
import BadgeEstado        from '@/components/common/BadgeEstado.vue'
import LoadingSpinner     from '@/components/common/LoadingSpinner.vue'

const authStore   = useAuthStore()
const pedidosStore = usePedidosStore()

const filtroEstado = ref('')
const filtroMina   = ref('')
const loading      = ref(false)

const pedidosFiltrados = computed(() =>
  pedidosStore.pedidos.filter(p => {
    const porEstado = !filtroEstado.value || p.estado === filtroEstado.value
    const porMina   = !filtroMina.value   ||
      p.mina.toLowerCase().includes(filtroMina.value.toLowerCase())
    return porEstado && porMina
  })
)

onMounted(async () => {
  loading.value = true
  await pedidosStore.fetchPedidos()
  loading.value = false
})

async function aprobar(id) {
  await pedidosStore.aprobarPedido(id, authStore.usuario.email)
}

async function rechazar(id) {
  const motivo = prompt('Motivo del rechazo:')
  if (motivo) await pedidosStore.rechazarPedido(id, motivo)
}

function formatFecha(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit', month: 'short', year: 'numeric'
  })
}
</script>
```

---

### `src/components/common/BadgeEstado.vue`

```vue
<template>
  <span :class="clases" class="px-2 py-1 rounded-full text-xs font-medium">
    {{ etiqueta }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ estado: String })

const config = {
  PENDIENTE:       { clase: 'bg-yellow-100 text-yellow-800', label: 'Pendiente' },
  APROBADO:        { clase: 'bg-blue-100 text-blue-800',     label: 'Aprobado' },
  EN_PREPARACION:  { clase: 'bg-purple-100 text-purple-800', label: 'En preparación' },
  DESPACHADO:      { clase: 'bg-indigo-100 text-indigo-800', label: 'Despachado' },
  ENTREGADO:       { clase: 'bg-green-100 text-green-800',   label: 'Entregado' },
  RECHAZADO:       { clase: 'bg-red-100 text-red-800',       label: 'Rechazado' },
  EN_RUTA:         { clase: 'bg-cyan-100 text-cyan-800',     label: 'En ruta' },
  PREPARANDO:      { clase: 'bg-orange-100 text-orange-800', label: 'Preparando' },
  DISPONIBLE:      { clase: 'bg-green-100 text-green-800',   label: 'Disponible' },
  RESERVADO:       { clase: 'bg-yellow-100 text-yellow-800', label: 'Reservado' },
  AGOTADO:         { clase: 'bg-red-100 text-red-800',       label: 'Agotado' },
  PENDIENTE_NOTIF: { clase: 'bg-orange-100 text-orange-800', label: 'Sin leer' },
  LEIDA:           { clase: 'bg-gray-100 text-gray-600',     label: 'Leída' },
}

const clases  = computed(() => config[props.estado]?.clase  || 'bg-gray-100 text-gray-600')
const etiqueta = computed(() => config[props.estado]?.label || props.estado)
</script>
```

---

## Paso 8 — Dockerizar el frontend

### `Dockerfile`

```dockerfile
# Etapa 1: build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json .
RUN npm install
COPY . .
RUN npm run build

# Etapa 2: servidor Nginx
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### `nginx.conf`

```nginx
server {
    listen 80;

    location / {
        root   /usr/share/nginx/html;
        index  index.html;
        try_files $uri $uri/ /index.html;  # para Vue Router
    }

    # Proxy al api-gateway
    location /api {
        proxy_pass http://api-gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Agregar al `docker-compose.yml`

```yaml
  frontend:
    build: ./madera-frontend
    container_name: frontend-madera
    ports:
      - "80:80"
    depends_on:
      - api-gateway
    networks:
      - madera-network
```

---

## Pasos de implementación

### Día 1 — Base del proyecto (4 horas)
1. Crear proyecto con `npm create vue@latest`
2. Instalar Tailwind CSS y configurar
3. Instalar Axios, Pinia, vue-chartjs
4. Configurar `api.js` con interceptors JWT
5. Configurar stores: `auth.js`, `pedidos.js`
6. Configurar router con guards
7. Crear layouts: `AuthLayout` y `DashboardLayout`
8. Crear `Sidebar` y `Navbar`
9. Verificar que el Login funciona y redirige al Dashboard

### Día 2 — Vistas principales (4 horas)
1. `LoginView` — formulario con manejo de errores
2. `DashboardView` — stats cards + gráfica + pedidos recientes
3. `InventarioView` — tabla con filtros
4. `InventarioFormView` — formulario para agregar madera
5. Componente `BadgeEstado` con todos los colores

### Día 3 — Pedidos y entregas (4 horas)
1. `PedidosView` — tabla con filtros, aprobar/rechazar inline
2. `PedidoFormView` — formulario para crear pedido
3. `PedidoDetailView` — detalle + timeline de estados
4. `EntregasView` — tabla de entregas en tránsito
5. `EntregaDetailView` — asignar transportista, marcar en ruta, confirmar

### Día 4 — Notificaciones, usuarios y Docker (3 horas)
1. `NotificacionesView` — lista con marcar como leída
2. `UsuariosView` — solo para ADMIN
3. Dockerizar con Nginx
4. Agregar al `docker-compose.yml`
5. Probar flujo completo dockerizado

---

## Checklist final

- [ ] Login funciona y guarda token en localStorage
- [ ] Sidebar muestra opciones según el rol
- [ ] Guard redirige a `/login` si no hay token
- [ ] Guard redirige a `/dashboard` si el rol no tiene permiso
- [ ] Dashboard muestra estadísticas reales de la API
- [ ] Inventario lista y permite agregar madera
- [ ] Pedidos lista con filtros por estado y mina
- [ ] COMPRAS puede crear pedidos
- [ ] ALMACEN puede aprobar y rechazar pedidos
- [ ] Al aprobar, el stock en inventario se reduce
- [ ] Entregas se crean automáticamente al aprobar un pedido
- [ ] TRANSPORTE puede marcar en ruta y confirmar entrega
- [ ] Notificaciones muestra badge con contador en el sidebar
- [ ] Token expirado redirige automáticamente al login
- [ ] Funciona dockerizado en `http://localhost`

---

## Comandos útiles

```bash
# Desarrollo
npm run dev          # inicia en http://localhost:5173

# Build para producción
npm run build

# Docker
docker build -t madera-frontend .
docker-compose up --build frontend
```

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
