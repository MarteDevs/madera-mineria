<template>
  <header class="h-16 bg-white border-b border-gray-100 flex items-center justify-between px-8 shrink-0 shadow-sm z-10">
    <!-- Left Section: Header Message / Date -->
    <div class="flex items-center space-x-4">
      <div class="hidden sm:block">
        <h2 class="text-sm font-semibold text-gray-800 leading-tight">
          {{ saludo }}, {{ authStore.usuario?.nombre || 'Usuario' }}
        </h2>
        <span class="text-[11px] text-gray-400 font-medium mt-0.5 block">
          {{ fechaActual }}
        </span>
      </div>
    </div>

    <!-- Right Section: Actions & Profile -->
    <div class="flex items-center space-x-6">
      <!-- Notification Icon Shortcut -->
      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen"
        to="/notificaciones"
        class="relative p-2 text-gray-400 hover:text-amber-600 hover:bg-amber-50 rounded-xl transition-all duration-200"
      >
        <span class="text-xl leading-none">🔔</span>
        <span
          v-if="notifStore.pendientes > 0"
          class="absolute top-1.5 right-1.5 w-4 h-4 bg-amber-600 text-white font-bold text-[9px] flex items-center justify-center rounded-full border-2 border-white"
        >
          {{ notifStore.pendientes }}
        </span>
      </RouterLink>

      <!-- Divider -->
      <div class="h-6 w-px bg-gray-100"></div>

      <!-- Profile Section -->
      <div class="flex items-center space-x-3">
        <div class="text-right">
          <p class="text-xs font-semibold text-gray-700 leading-none">
            {{ authStore.usuario?.nombre || 'Usuario' }}
          </p>
          <span class="text-[10px] text-gray-400 font-semibold uppercase mt-1 block">
            {{ authStore.usuario?.email }}
          </span>
        </div>
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500 to-amber-700 text-white flex items-center justify-center font-bold text-sm shadow-md shadow-amber-950/20 select-none uppercase">
          {{ primeraLetra }}
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificacionesStore } from '@/stores/notificaciones'

const authStore = useAuthStore()
const notifStore = useNotificacionesStore()

const saludo = computed(() => {
  const horas = new Date().getHours()
  if (horas < 12) return 'Buenos días'
  if (horas < 19) return 'Buenas tardes'
  return 'Buenas noches'
})

const fechaActual = computed(() => {
  return new Date().toLocaleDateString('es-PE', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})

const primeraLetra = computed(() => {
  return authStore.usuario?.nombre?.charAt(0) || 'U'
})
</script>
