<template>
  <div class="fixed top-6 right-6 z-55 space-y-3 w-full max-w-sm pointer-events-none">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="pointer-events-auto flex items-start gap-3 p-4 rounded-xl border shadow-lg backdrop-blur-md transition-all duration-300 transform"
        :class="estilos[toast.tipo]?.contenedor || estilos.info.contenedor"
      >
        <span class="text-base flex-shrink-0 mt-0.5">{{ estilos[toast.tipo]?.icono }}</span>
        <div class="flex-1 text-xs font-semibold leading-normal">
          {{ toast.mensaje }}
        </div>
        <button
          @click="toastStore.remove(toast.id)"
          class="text-stone-400 hover:text-stone-600 transition-colors ml-2 cursor-pointer flex-shrink-0 text-xs"
        >
          ✕
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()

const estilos = {
  success: {
    contenedor: 'bg-emerald-50/90 border-emerald-150 text-emerald-800',
    icono: '✅'
  },
  error: {
    contenedor: 'bg-red-50/90 border-red-150 text-red-800',
    icono: '❌'
  },
  warning: {
    contenedor: 'bg-amber-50/90 border-amber-150 text-amber-800',
    icono: '⚠️'
  },
  info: {
    contenedor: 'bg-stone-805/95 border-stone-750 text-stone-100 shadow-stone-900/30',
    icono: '🔔'
  }
}
</script>

<style scoped>
.toast-enter-from {
  opacity: 0;
  transform: translateY(-20px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}
.toast-leave-active {
  position: absolute;
  width: 100%;
}
</style>
