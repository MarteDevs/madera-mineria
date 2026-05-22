<template>
  <span :class="clases" class="px-2 py-1 rounded-full text-xs font-semibold uppercase tracking-wider select-none">
    {{ etiqueta }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  estado: {
    type: String,
    required: true
  }
})

const config = {
  // Pedidos & Entregas
  PENDIENTE:       { clase: 'bg-amber-100 text-amber-800 border border-amber-200', label: 'Pendiente' },
  APROBADO:        { clase: 'bg-blue-100 text-blue-800 border border-blue-200',     label: 'Aprobado' },
  EN_PREPARACION:  { clase: 'bg-purple-100 text-purple-800 border border-purple-200', label: 'En preparación' },
  DESPACHADO:      { clase: 'bg-indigo-100 text-indigo-800 border border-indigo-200', label: 'Despachado' },
  ENTREGADO:       { clase: 'bg-emerald-100 text-emerald-800 border border-emerald-200', label: 'Entregado' },
  RECHAZADO:       { clase: 'bg-red-100 text-red-800 border border-red-200',       label: 'Rechazado' },
  
  // Entregas específicas
  PREPARANDO:      { clase: 'bg-orange-100 text-orange-800 border border-orange-200', label: 'Preparando' },
  EN_RUTA:         { clase: 'bg-cyan-100 text-cyan-800 border border-cyan-200',     label: 'En ruta' },
  FALLIDO:         { clase: 'bg-rose-100 text-rose-800 border border-rose-200',     label: 'Fallido' },

  // Inventario
  DISPONIBLE:      { clase: 'bg-emerald-100 text-emerald-800 border border-emerald-200', label: 'Disponible' },
  RESERVADO:       { clase: 'bg-amber-100 text-amber-800 border border-amber-200', label: 'Reservado' },
  AGOTADO:         { clase: 'bg-red-100 text-red-800 border border-red-200',       label: 'Agotado' },
  
  // Notificaciones
  PENDIENTE_NOTIF: { clase: 'bg-orange-100 text-orange-800 border border-orange-200', label: 'Sin leer' },
  LEIDA:           { clase: 'bg-gray-100 text-gray-600 border border-gray-200',     label: 'Leída' }
}

const clases  = computed(() => config[props.estado]?.clase  || 'bg-gray-100 text-gray-600 border border-gray-200')
const etiqueta = computed(() => config[props.estado]?.label || props.estado)
</script>
