<template>
  <aside class="w-64 bg-stone-900 text-stone-300 flex flex-col shrink-0 border-r border-stone-800 shadow-xl">
    <!-- Brand / Header -->
    <div class="h-16 flex items-center px-6 border-b border-stone-800 bg-stone-950/40">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-amber-600 flex items-center justify-center text-lg shadow-md shadow-amber-900/30">
          🪵
        </div>
        <div>
          <h1 class="font-bold text-white leading-none text-sm tracking-wider uppercase">Madera Minería</h1>
          <span class="text-[10px] text-stone-500 font-semibold tracking-widest uppercase mt-0.5 block">Gestión Interna</span>
        </div>
      </div>
    </div>

    <!-- Active User Profile Summary -->
    <div class="p-6 border-b border-stone-800/60 bg-stone-950/10">
      <p class="text-[10px] font-semibold text-stone-500 uppercase tracking-wider">Usuario Conectado</p>
      <h3 class="font-medium text-white text-sm mt-1 truncate">{{ authStore.nombreCompleto }}</h3>
      <div class="mt-2 flex items-center">
        <span class="text-[10px] font-semibold tracking-wider bg-amber-950/60 text-amber-500 border border-amber-900/60 px-2 py-0.5 rounded-full uppercase">
          {{ rolLegible }}
        </span>
      </div>
    </div>

    <!-- Navigation links -->
    <nav class="flex-1 p-4 space-y-1.5 overflow-y-auto">
      <RouterLink to="/dashboard" class="nav-link">
        <span class="text-lg">📊</span>
        <span>Dashboard</span>
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen"
        to="/inventario"
        class="nav-link"
      >
        <span class="text-lg">📦</span>
        <span>Inventario</span>
      </RouterLink>

      <RouterLink to="/pedidos" class="nav-link">
        <span class="text-lg">📋</span>
        <span>Pedidos</span>
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen || authStore.esTransporte"
        to="/entregas"
        class="nav-link"
      >
        <span class="text-lg">🚛</span>
        <span>Entregas</span>
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen"
        to="/notificaciones"
        class="nav-link"
      >
        <span class="text-lg">🔔</span>
        <span>Notificaciones</span>
        <span
          v-if="notifStore.pendientes > 0"
          class="ml-auto bg-amber-600 text-white font-bold text-[10px] px-2 py-0.5 rounded-full shadow-sm shadow-amber-950"
        >
          {{ notifStore.pendientes }}
        </span>
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen"
        to="/proveedores"
        class="nav-link"
      >
        <span class="text-lg">🏭</span>
        <span>Proveedores</span>
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen"
        to="/mantenimiento"
        class="nav-link"
      >
        <span class="text-lg">🔧</span>
        <span>Mantenimiento</span>
      </RouterLink>


      <RouterLink
        v-if="authStore.esAdmin"
        to="/reportes"
        class="nav-link"
      >
        <span class="text-lg">📊</span>
        <span>Reportes</span>
      </RouterLink>

      <RouterLink
        v-if="authStore.esAdmin"
        to="/usuarios"
        class="nav-link"
      >
        <span class="text-lg">👥</span>
        <span>Usuarios</span>
      </RouterLink>
    </nav>

    <!-- Logout footer -->
    <div class="p-4 border-t border-stone-800 bg-stone-950/20">
      <button
        @click="handleLogout"
        class="w-full flex items-center gap-3 px-3 py-2 text-sm font-medium text-stone-400 hover:text-rose-400 hover:bg-rose-950/20 hover:border-rose-900/30 border border-transparent rounded-xl transition-all duration-200"
      >
        <span class="text-lg">🚪</span>
        <span>Cerrar sesión</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificacionesStore } from '@/stores/notificaciones'

const authStore = useAuthStore()
const notifStore = useNotificacionesStore()
const router = useRouter()

const rolLegible = computed(() => {
  const mapa = {
    ROLE_ADMIN: 'Administrador',
    ROLE_ALMACEN: 'Jefe de Almacén',
    ROLE_COMPRAS: 'Encargado Compras',
    ROLE_TRANSPORTE: 'Transportista / Logística'
  }
  return mapa[authStore.rol] || authStore.rol
})

onMounted(() => {
  if (authStore.esAdmin || authStore.esAlmacen) {
    notifStore.fetchPendientesCount()
    
    // Iniciar conexión push en tiempo real usando Server-Sent Events (SSE)
    notifStore.conectarSSE()
  }
})

onUnmounted(() => {
  notifStore.desconectarSSE()
})

function handleLogout() {
  notifStore.desconectarSSE()
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
@reference "tailwindcss";

.nav-link {
  @apply flex items-center gap-3.5 px-4 py-2.5 rounded-xl text-sm font-medium
         text-stone-400 hover:text-white hover:bg-stone-800/40 border border-transparent
         transition-all duration-150;
}
.router-link-active {
  @apply bg-amber-600/10 text-amber-500 border-amber-950/60 font-semibold shadow-inner;
}
</style>
