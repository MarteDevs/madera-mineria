<template>
  <div class="max-w-4xl mx-auto space-y-6 animate-fade-in">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Notificaciones del Sistema</h1>
        <p class="text-stone-500 text-sm mt-1">
          Registro de alertas y eventos automáticos sobre la asignación y despacho de madera en mina.
        </p>
      </div>
      <div v-if="notificacionesStore.pendientes > 0" class="bg-amber-100 text-amber-800 text-xs font-bold px-3 py-1.5 rounded-full flex items-center gap-1.5">
        <span class="w-2 h-2 bg-amber-500 rounded-full animate-ping"></span>
        {{ notificacionesStore.pendientes }} Pendientes
      </div>
    </div>

    <!-- Error state -->
    <AlertMessage v-if="notificacionesStore.error" tipo="error">
      {{ notificacionesStore.error }}
    </AlertMessage>

    <!-- Filtros de Estado -->
    <div class="flex items-center justify-between border-b border-stone-200 pb-2">
      <div class="flex gap-2">
        <button
          @click="filtroEstado = 'TODAS'"
          class="px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all"
          :class="filtroEstado === 'TODAS' ? 'bg-stone-900 text-white shadow-sm' : 'text-stone-600 hover:bg-stone-100'"
        >
          Todas
        </button>
        <button
          @click="filtroEstado = 'PENDIENTES'"
          class="px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all"
          :class="filtroEstado === 'PENDIENTES' ? 'bg-stone-900 text-white shadow-sm' : 'text-stone-600 hover:bg-stone-100'"
        >
          Pendientes
        </button>
        <button
          @click="filtroEstado = 'LEIDAS'"
          class="px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all"
          :class="filtroEstado === 'LEIDAS' ? 'bg-stone-900 text-white shadow-sm' : 'text-stone-600 hover:bg-stone-100'"
        >
          Leídas
        </button>
      </div>
      
      <button
        v-if="notificacionesPendientes.length > 0"
        @click="marcarTodasComoLeidas"
        :disabled="bulkLoading"
        class="text-xs text-amber-650 hover:text-amber-700 font-bold flex items-center gap-1 bg-transparent border-none"
      >
        <span v-if="bulkLoading">Procesando...</span>
        <span v-else>✓ Marcar todas como leídas</span>
      </button>
    </div>

    <!-- Loading spinner -->
    <div v-if="notificacionesStore.loading && notificacionesStore.notificaciones.length === 0" class="min-h-[200px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <!-- Listado -->
    <div v-else class="space-y-4">
      <div
        v-for="notif in notificacionesFiltradas"
        :key="notif.id"
        class="card bg-white p-5 border transition-all duration-200"
        :class="notif.estado === 'LEIDA' ? 'opacity-70 border-stone-150' : 'border-amber-200 bg-amber-50/10 shadow-sm'"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="flex items-start gap-3.5">
            <!-- Icono según tipo de evento -->
            <div
              class="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 text-lg"
              :class="obtenerClaseIcono(notif.tipoEvento)"
            >
              {{ obtenerIcono(notif.tipoEvento) }}
            </div>

            <!-- Contenido -->
            <div class="space-y-1">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="text-xs font-bold uppercase tracking-wider text-stone-500">
                  {{ formatTipoEvento(notif.tipoEvento) }}
                </span>
                <span class="text-[10px] text-stone-400 font-medium">•</span>
                <span class="text-[11px] text-stone-400 font-semibold">{{ formatFecha(notif.fechaRecibida) }}</span>
                <span v-if="notif.estado === 'PENDIENTE'" class="bg-amber-100 text-amber-800 text-[10px] font-bold px-2 py-0.5 rounded">
                  Nueva
                </span>
              </div>
              
              <p class="text-sm font-semibold text-stone-900 leading-snug">
                {{ notif.mensaje }}
              </p>

              <!-- Enlaces e info secundaria -->
              <div class="flex items-center gap-3 pt-1 text-xs text-stone-500 font-medium">
                <span v-if="notif.mina" class="flex items-center gap-1 text-stone-600 bg-stone-100 px-2 py-0.5 rounded text-[11px]">
                  ⛰️ {{ notif.mina }}
                </span>
                <RouterLink
                  v-if="notif.pedidoId"
                  :to="`/pedidos/${notif.pedidoId}`"
                  class="text-amber-650 hover:underline font-bold text-[11px] inline-flex items-center gap-0.5"
                >
                  Pedido #{{ notif.pedidoId }} ➔
                </RouterLink>
              </div>
            </div>
          </div>

          <!-- Acciones -->
          <div v-if="notif.estado === 'PENDIENTE'" class="flex-shrink-0">
            <button
              @click="marcarComoLeida(notif.id)"
              :disabled="loadingId === notif.id"
              class="text-xs border border-stone-200 hover:border-amber-450 hover:bg-amber-50 hover:text-amber-700 px-3 py-1.5 rounded-lg font-semibold transition-all bg-white"
            >
              <span v-if="loadingId === notif.id">Leyendo...</span>
              <span v-else>Marcar leída</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Estado vacío -->
      <div v-if="notificacionesFiltradas.length === 0" class="card bg-white p-12 text-center border border-stone-150">
        <p class="text-3xl mb-3">🔔</p>
        <h3 class="text-sm font-bold text-stone-800">No hay notificaciones</h3>
        <p class="text-stone-400 text-xs mt-1">
          Todo al día. No se encontraron alertas para el filtro seleccionado.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useNotificacionesStore } from '@/stores/notificaciones'
import { useDialogStore } from '@/stores/dialog'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const notificacionesStore = useNotificacionesStore()
const dialogStore = useDialogStore()
const filtroEstado = ref('PENDIENTES')
const loadingId = ref(null)
const bulkLoading = ref(false)

onMounted(async () => {
  await notificacionesStore.fetchNotificaciones()
  await notificacionesStore.fetchPendientesCount()
})

const notificacionesFiltradas = computed(() => {
  const notifs = notificacionesStore.notificaciones || []
  if (filtroEstado.value === 'PENDIENTES') {
    return notifs.filter(n => n.estado === 'PENDIENTE')
  } else if (filtroEstado.value === 'LEIDAS') {
    return notifs.filter(n => n.estado === 'LEIDA')
  }
  return notifs
})

const notificacionesPendientes = computed(() => {
  return (notificacionesStore.notificaciones || []).filter(n => n.estado === 'PENDIENTE')
})

async function marcarComoLeida(id) {
  loadingId.value = id
  try {
    await notificacionesStore.marcarComoLeida(id)
  } catch (err) {
    dialogStore.alert({
      titulo: 'Error',
      mensaje: err.message || 'Error al marcar como leída.',
      tipo: 'error'
    })
  } finally {
    loadingId.value = null
  }
}

async function marcarTodasComoLeidas() {
  const pendientesList = notificacionesPendientes.value
  if (pendientesList.length === 0) return
  const confirmado = await dialogStore.confirm({
    titulo: 'Marcar Todas como Leídas',
    mensaje: '¿Desea marcar todas las notificaciones pendientes como leídas?',
    confirmLabel: 'Sí, Marcar Todas',
    cancelLabel: 'Cancelar'
  })
  if (!confirmado) return

  bulkLoading.value = true
  try {
    for (const notif of pendientesList) {
      await notificacionesStore.marcarComoLeida(notif.id)
    }
  } catch (err) {
    console.error('Error en marcado masivo:', err)
  } finally {
    bulkLoading.value = false
  }
}

function obtenerIcono(tipo) {
  if (!tipo) return '🔔'
  const t = tipo.toUpperCase()
  if (t.includes('CREACION') || t.includes('CREAR') || t.includes('CREADO')) return '📋'
  if (t.includes('APROB') || t.includes('APROBAR') || t.includes('APROBADO')) return '✅'
  if (t.includes('RECHAZ') || t.includes('RECHAZADO')) return '❌'
  if (t.includes('PREPAR') || t.includes('PREPARANDO')) return '📦'
  if (t.includes('RUTA') || t.includes('LOGISTICA') || t.includes('DESPACHO') || t.includes('EN_RUTA')) return '🚚'
  if (t.includes('ENTREG') || t.includes('RECEPCION') || t.includes('ENTREGADO') || t.includes('RECEPCIONADO')) return '🤝'
  if (t.includes('STOCK') || t.includes('MINIMO') || t.includes('INVENTARIO')) return '⚠️'
  return '🔔'
}

function obtenerClaseIcono(tipo) {
  if (!tipo) return 'bg-stone-100 text-stone-700'
  const t = tipo.toUpperCase()
  if (t.includes('CREACION') || t.includes('CREAR') || t.includes('CREADO')) return 'bg-amber-100 text-amber-700'
  if (t.includes('APROB') || t.includes('APROBAR') || t.includes('APROBADO')) return 'bg-emerald-100 text-emerald-700'
  if (t.includes('RECHAZ') || t.includes('RECHAZADO')) return 'bg-red-100 text-red-700'
  if (t.includes('PREPAR') || t.includes('PREPARANDO')) return 'bg-orange-100 text-orange-700'
  if (t.includes('RUTA') || t.includes('LOGISTICA') || t.includes('DESPACHO') || t.includes('EN_RUTA')) return 'bg-indigo-100 text-indigo-700'
  if (t.includes('ENTREG') || t.includes('RECEPCION') || t.includes('ENTREGADO') || t.includes('RECEPCIONADO')) return 'bg-emerald-100 text-emerald-700'
  if (t.includes('STOCK') || t.includes('MINIMO') || t.includes('INVENTARIO')) return 'bg-red-50 text-red-650 border border-red-150'
  return 'bg-stone-100 text-stone-700'
}

function formatTipoEvento(tipo) {
  if (!tipo) return 'Notificación'
  return tipo.replace(/_/g, ' ')
}

function formatFecha(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>
