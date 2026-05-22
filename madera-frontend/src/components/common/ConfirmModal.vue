<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-950/60 backdrop-blur-md animate-fade-in"
  >
    <!-- Modal Container -->
    <div
      class="bg-white/95 border border-stone-200/80 w-full max-w-md rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh] text-left animate-scale-in"
    >
      <!-- Top Accent Line -->
      <div :class="['h-1.5 w-full', tipoEsPeligroso ? 'bg-rose-500' : 'bg-amber-600']"></div>

      <!-- Header -->
      <div class="px-6 pt-5 pb-3 border-b border-stone-100 flex items-center justify-between">
        <h3 class="text-base font-bold text-stone-900 tracking-tight">{{ titulo }}</h3>
        <button @click="cancelar" class="text-stone-400 hover:text-stone-700 transition-colors text-xl font-medium p-1 leading-none rounded-lg hover:bg-stone-100/50">
          &times;
        </button>
      </div>

      <!-- Body -->
      <div class="px-6 py-5 flex-1 overflow-y-auto text-sm text-stone-600 leading-relaxed">
        <slot name="body">
          <slot>
            <p class="font-medium text-stone-700">{{ mensaje }}</p>
          </slot>
        </slot>
        
        <!-- Input field for prompting extra text (like reason for rejection) -->
        <div v-if="conInput" class="mt-4">
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-2">
            {{ inputLabel }}
          </label>
          <input
            v-model="inputValue"
            type="text"
            :placeholder="inputPlaceholder"
            class="w-full border border-stone-300 rounded-xl px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent text-sm transition-all duration-200 placeholder:text-stone-400 bg-stone-50/50"
            required
            @keyup.enter="confirmar"
          />
        </div>
      </div>

      <!-- Footer -->
      <div class="px-6 py-4 bg-stone-50/80 border-t border-stone-100 flex justify-end items-center gap-3">
        <button
          v-if="mostrarCancelar"
          @click="cancelar"
          class="px-4 py-2 text-stone-600 hover:text-stone-850 font-semibold text-sm transition-all hover:bg-stone-200/50 rounded-xl border border-stone-200 hover:border-stone-300"
        >
          {{ computedCancelLabel }}
        </button>
        <button
          @click="confirmar"
          :disabled="computedLoading || (conInput && !inputValue.trim())"
          :class="[
            'px-5 py-2 font-bold text-sm rounded-xl transition-all shadow-sm active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed',
            tipoEsPeligroso ? 'bg-rose-600 hover:bg-rose-700 text-white shadow-rose-250/20' : 'bg-amber-600 hover:bg-amber-700 text-white shadow-amber-250/20'
          ]"
        >
          <span v-if="computedLoading">Procesando...</span>
          <span v-else>{{ computedConfirmLabel }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  abierto: {
    type: Boolean,
    default: true // Allows using v-if directly on the component
  },
  titulo: {
    type: String,
    default: 'Confirmar acción'
  },
  mensaje: {
    type: String,
    default: ''
  },
  txtConfirmar: {
    type: String,
    default: 'Confirmar'
  },
  confirmLabel: {
    type: String,
    default: null
  },
  txtCancelar: {
    type: String,
    default: 'Cancelar'
  },
  cancelLabel: {
    type: String,
    default: null
  },
  mostrarCancelar: {
    type: Boolean,
    default: true
  },
  tipoEsPeligroso: {
    type: Boolean,
    default: false
  },
  conInput: {
    type: Boolean,
    default: false
  },
  inputLabel: {
    type: String,
    default: 'Motivo / Comentarios:'
  },
  inputPlaceholder: {
    type: String,
    default: 'Escribe aquí...'
  },
  cargando: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['confirm', 'cancel'])

const inputValue = ref('')

const computedConfirmLabel = computed(() => props.confirmLabel || props.txtConfirmar)
const computedCancelLabel = computed(() => props.cancelLabel || props.txtCancelar)
const computedLoading = computed(() => props.loading || props.cargando)

function cancelar() {
  inputValue.value = ''
  emit('cancel')
}

function confirmar() {
  emit('confirm', inputValue.value)
  inputValue.value = ''
}
</script>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.animate-fade-in {
  animation: fadeIn 0.2s ease-out forwards;
}

.animate-scale-in {
  animation: scaleIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}
</style>
