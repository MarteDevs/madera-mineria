<template>
  <div
    v-if="mostrar && mensaje"
    :class="[
      'p-4 rounded-xl border flex items-start space-x-3 transition-all duration-300 animate-fade-in mb-4',
      tipoClases
    ]"
  >
    <!-- Icon -->
    <span class="text-xl select-none">{{ icono }}</span>
    
    <div class="flex-1">
      <h4 class="text-sm font-semibold">{{ titulo || tipoTitulo }}</h4>
      <p class="text-xs mt-0.5 opacity-90">{{ mensaje }}</p>
    </div>

    <!-- Close button -->
    <button
      v-if="descartable"
      @click="cerrar"
      class="text-gray-400 hover:text-gray-700 transition-colors text-lg leading-none"
    >
      &times;
    </button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  mensaje: {
    type: String,
    required: true
  },
  tipo: {
    type: String,
    default: 'error', // error, success, info, warning
  },
  titulo: {
    type: String,
    default: ''
  },
  descartable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['close'])

const mostrar = ref(true)

const tipoClases = computed(() => {
  const map = {
    error: 'bg-red-50 text-red-800 border-red-200',
    success: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    warning: 'bg-amber-50 text-amber-800 border-amber-200',
    info: 'bg-blue-50 text-blue-800 border-blue-200'
  }
  return map[props.tipo] || map.error
})

const tipoTitulo = computed(() => {
  const map = {
    error: 'Error',
    success: 'Operación Exitosa',
    warning: 'Advertencia',
    info: 'Información'
  }
  return map[props.tipo] || 'Mensaje'
})

const icono = computed(() => {
  const map = {
    error: '❌',
    success: '✅',
    warning: '⚠️',
    info: 'ℹ️'
  }
  return map[props.tipo] || '🔔'
})

function cerrar() {
  mostrar.value = false
  emit('close')
}
</script>
