<template>
  <Transition name="fade">
    <div
      v-if="dialogStore.abierto"
      class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-stone-950/60 backdrop-blur-md"
      @click.self="dialogStore.cancelar"
    >
      <Transition name="scale">
        <!-- Modal Card Container -->
        <div
          v-if="dialogStore.abierto"
          class="bg-white/95 border border-stone-200/80 w-full max-w-md rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh] text-left"
        >
          <!-- Top Accent Line depending on status -->
          <div :class="['h-1.5 w-full', accentBarClass]"></div>

          <!-- Header -->
          <div class="px-6 pt-5 pb-3 flex items-start justify-between">
            <div class="flex items-center gap-3">
              <span :class="['p-2 rounded-xl flex items-center justify-center text-xl shadow-sm border', iconContainerClass]">
                {{ iconSymbol }}
              </span>
              <h3 class="text-base font-bold text-stone-900 tracking-tight">
                {{ dialogStore.titulo }}
              </h3>
            </div>
            <button
              @click="dialogStore.cancelar"
              class="text-stone-400 hover:text-stone-700 transition-colors text-xl font-medium p-1 leading-none rounded-lg hover:bg-stone-100/50"
            >
              &times;
            </button>
          </div>

          <!-- Body -->
          <div class="px-6 py-4 flex-1 overflow-y-auto text-sm text-stone-600 leading-relaxed">
            <p class="whitespace-pre-wrap font-medium text-stone-700">{{ dialogStore.mensaje }}</p>

            <!-- Input (Prompt dialog) -->
            <div v-if="dialogStore.conInput" class="mt-4">
              <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-2">
                {{ dialogStore.inputLabel }}
              </label>
              <input
                v-model="dialogStore.inputValue"
                type="text"
                :placeholder="dialogStore.inputPlaceholder"
                class="w-full border border-stone-300 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent text-sm transition-all duration-200 placeholder:text-stone-400 bg-stone-50/50"
                @keyup.enter="dialogStore.aceptar()"
                ref="inputRef"
              />
            </div>
          </div>

          <!-- Footer -->
          <div class="px-6 py-4 bg-stone-50/80 border-t border-stone-100 flex justify-end items-center gap-3">
            <button
              v-if="dialogStore.cancelLabel"
              @click="dialogStore.cancelar"
              class="px-4 py-2 text-stone-600 hover:text-stone-850 font-semibold text-sm transition-all hover:bg-stone-200/50 rounded-xl border border-stone-200 hover:border-stone-300"
            >
              {{ dialogStore.cancelLabel }}
            </button>
            <button
              @click="dialogStore.aceptar()"
              :disabled="dialogStore.conInput && !dialogStore.inputValue.trim()"
              :class="[
                'px-5 py-2 font-bold text-sm rounded-xl transition-all shadow-sm active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed',
                confirmButtonClass
              ]"
            >
              {{ dialogStore.confirmLabel }}
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useDialogStore } from '@/stores/dialog'

const dialogStore = useDialogStore()
const inputRef = ref(null)

// Focus the input element if it opens
watch(
  () => dialogStore.abierto,
  async (val) => {
    if (val && dialogStore.conInput) {
      await nextTick()
      if (inputRef.value) {
        inputRef.value.focus()
      }
    }
  }
)

const accentBarClass = computed(() => {
  switch (dialogStore.tipo) {
    case 'success':
      return 'bg-emerald-500'
    case 'error':
      return 'bg-rose-500'
    case 'warning':
      return 'bg-amber-500'
    case 'info':
      return 'bg-blue-500'
    case 'confirm':
    default:
      return 'bg-amber-600' // Wood theme accent
  }
})

const iconContainerClass = computed(() => {
  switch (dialogStore.tipo) {
    case 'success':
      return 'bg-emerald-50 border-emerald-100 text-emerald-600'
    case 'error':
      return 'bg-rose-50 border-rose-100 text-rose-600'
    case 'warning':
      return 'bg-amber-50 border-amber-100 text-amber-600'
    case 'info':
      return 'bg-blue-50 border-blue-100 text-blue-600'
    case 'confirm':
    default:
      return 'bg-amber-50 border-amber-100 text-amber-600'
  }
})

const iconSymbol = computed(() => {
  switch (dialogStore.tipo) {
    case 'success':
      return '✓'
    case 'error':
      return '✕'
    case 'warning':
      return '⚠️'
    case 'info':
      return 'ℹ️'
    case 'confirm':
    default:
      return '❓'
  }
})

const confirmButtonClass = computed(() => {
  if (dialogStore.tipoEsPeligroso) {
    return 'bg-rose-600 hover:bg-rose-700 text-white shadow-rose-250/20 hover:shadow-md'
  }
  switch (dialogStore.tipo) {
    case 'success':
      return 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-250/20 hover:shadow-md'
    case 'error':
      return 'bg-rose-600 hover:bg-rose-700 text-white shadow-rose-250/20 hover:shadow-md'
    case 'warning':
      return 'bg-amber-600 hover:bg-amber-700 text-white shadow-amber-250/20 hover:shadow-md'
    case 'confirm':
    default:
      return 'bg-amber-600 hover:bg-amber-700 text-white shadow-amber-250/20 hover:shadow-md'
  }
})
</script>

<style scoped>
/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.scale-enter-active,
.scale-leave-active {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.25s ease;
}

.scale-enter-from,
.scale-leave-to {
  transform: scale(0.95);
  opacity: 0;
}
</style>
