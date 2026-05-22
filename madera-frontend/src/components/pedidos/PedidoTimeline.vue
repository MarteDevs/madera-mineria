<template>
  <div class="space-y-6">
    <h3 class="text-sm font-bold text-stone-900 uppercase tracking-wider border-b border-stone-100 pb-2">
      Línea de Tiempo del Pedido
    </h3>

    <div v-if="!historial || historial.length === 0" class="text-xs text-stone-400 italic">
      No se registran transiciones de estado para este pedido.
    </div>

    <div v-else class="relative border-l border-stone-200 ml-3 space-y-6 pb-2 pl-6">
      <div
        v-for="item in ordenadoPorFecha"
        :key="item.id"
        class="relative text-xs text-stone-600"
      >
        <!-- Dot indicator -->
        <span
          class="absolute -left-[30px] top-0 w-3 h-3 rounded-full border-2 border-white ring-4 ring-offset-0"
          :class="getDotRingClass(item.estadoNuevo)"
        ></span>

        <div>
          <p class="font-bold text-stone-850">
            Estado Cambiado a:
            <BadgeEstado :estado="item.estadoNuevo" class="ml-1.5" />
          </p>
          <p class="text-[10px] text-stone-400 mt-1">
            {{ formatFecha(item.fechaCambio || item.fechaRegistro) }}
          </p>

          <div class="mt-2 bg-stone-50 border border-stone-100 rounded-xl p-3 text-stone-600 space-y-1">
            <p><b>Responsable:</b> {{ item.responsable }}</p>
            <p v-if="item.estadoAnterior"><b>Estado Anterior:</b> <span class="capitalize text-stone-400 font-medium">{{ item.estadoAnterior }}</span></p>
            <p v-if="item.motivo"><b>Detalle / Motivo:</b> {{ item.motivo }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BadgeEstado from '@/components/common/BadgeEstado.vue'

const props = defineProps({
  historial: {
    type: Array,
    default: () => []
  }
})

const ordenadoPorFecha = computed(() => {
  if (!props.historial) return []
  const list = [...props.historial]
  list.sort((a, b) => {
    const da = new Date(a.fechaCambio || a.fechaRegistro || 0).getTime()
    const db = new Date(b.fechaCambio || b.fechaRegistro || 0).getTime()
    return db - da // Recientes arriba
  })
  return list
})

function formatFecha(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getDotRingClass(estado) {
  const map = {
    PENDIENTE: 'bg-yellow-500 ring-yellow-100',
    APROBADO: 'bg-blue-500 ring-blue-100',
    EN_PREPARACION: 'bg-purple-500 ring-purple-100',
    DESPACHADO: 'bg-indigo-500 ring-indigo-100',
    ENTREGADO: 'bg-emerald-500 ring-emerald-100',
    RECHAZADO: 'bg-rose-500 ring-rose-100'
  }
  return map[estado] || 'bg-stone-500 ring-stone-100'
}
</script>
