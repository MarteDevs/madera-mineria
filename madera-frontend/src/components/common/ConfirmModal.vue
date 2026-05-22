<template>
  <div
    v-if="abierto"
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm"
  >
    <!-- Modal Container -->
    <div
      class="bg-white w-full max-w-md rounded-2xl shadow-2xl border border-gray-100 overflow-hidden animate-fade-in"
    >
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
        <h3 class="text-lg font-bold text-gray-900">{{ titulo }}</h3>
        <button @click="cancelar" class="text-gray-400 hover:text-gray-600 text-xl font-bold">
          &times;
        </button>
      </div>

      <!-- Body -->
      <div class="px-6 py-6 text-sm text-gray-600">
        <p>{{ mensaje }}</p>
        
        <!-- Input field for prompting extra text (like reason for rejection) -->
        <div v-if="conInput" class="mt-4">
          <label class="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
            {{ inputLabel }}
          </label>
          <input
            v-model="inputValue"
            type="text"
            :placeholder="inputPlaceholder"
            class="input-field"
            required
          />
        </div>
      </div>

      <!-- Footer -->
      <div class="px-6 py-4 bg-gray-50 border-t border-gray-100 flex justify-end space-x-3">
        <button @click="cancelar" class="btn-secondary">
          {{ txtCancelar }}
        </button>
        <button
          @click="confirmar"
          :disabled="cargando || (conInput && !inputValue.trim())"
          :class="[tipoEsPeligroso ? 'btn-danger' : 'btn-primary']"
        >
          <span v-if="cargando">Procesando...</span>
          <span v-else>{{ txtConfirmar }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  abierto: {
    type: Boolean,
    required: true
  },
  titulo: {
    type: String,
    default: 'Confirmar acción'
  },
  mensaje: {
    type: String,
    required: true
  },
  txtConfirmar: {
    type: String,
    default: 'Confirmar'
  },
  txtCancelar: {
    type: String,
    default: 'Cancelar'
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
  }
})

const emit = defineEmits(['confirm', 'cancel'])

const inputValue = ref('')

function cancelar() {
  inputValue.value = ''
  emit('cancel')
}

function confirmar() {
  emit('confirm', inputValue.value)
  inputValue.value = ''
}
</script>
