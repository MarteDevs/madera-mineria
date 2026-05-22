<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <RouterLink to="/pedidos" class="text-xs text-stone-500 hover:text-stone-750 font-semibold flex items-center gap-1.5 mb-2">
          ⬅ Volver al listado de pedidos
        </RouterLink>
        <h1 class="text-2xl font-bold text-stone-900">Pedido #{{ pedidoId }}</h1>
      </div>
      <div v-if="pedido" class="flex items-center gap-3">
        <BadgeEstado :estado="pedido.estado" class="text-sm px-3 py-1.5" />
      </div>
    </div>

    <!-- Error state -->
    <AlertMessage v-if="error" tipo="error">
      {{ error }}
    </AlertMessage>

    <!-- Loading -->
    <div v-if="loading" class="min-h-[300px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <!-- Detail Content -->
    <div v-else-if="pedido" class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- Pedido Specs Card -->
      <div class="card bg-white p-6 md:col-span-2 space-y-6">
        <div>
          <h2 class="text-base font-bold text-stone-900 border-b border-stone-100 pb-3 mb-4">Información del Suministro</h2>
          <div class="grid grid-cols-2 gap-4 text-sm text-stone-600">
            <div>
              <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Tipo de Madera</p>
              <p class="font-bold text-stone-850 capitalize mt-1 text-base">{{ pedido.tipoMadera }}</p>
            </div>
            <div>
              <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Cantidad Solicitada</p>
              <p class="font-bold text-stone-850 mt-1 text-base">{{ pedido.cantidadSolicitada }} {{ pedido.unidad || 'und' }}</p>
            </div>
            <div>
              <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Mina de Destino</p>
              <p class="font-medium text-stone-800 mt-1">{{ pedido.mina }}</p>
            </div>
            <div>
              <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Fecha de Solicitud</p>
              <p class="text-stone-700 mt-1">{{ formatFechaCompleta(pedido.fechaPedido) }}</p>
            </div>
          </div>
        </div>

        <!-- Acciones especiales para Almacén -->
        <div
          v-if="authStore.esAlmacen && pedido.estado === 'PENDIENTE'"
          class="bg-stone-50 rounded-xl p-4 border border-stone-150 flex items-center justify-between gap-4"
        >
          <div>
            <p class="text-xs font-bold text-stone-800">Evaluación de Solicitud</p>
            <p class="text-[11px] text-stone-500 mt-0.5">El stock se deducirá al aprobar.</p>
          </div>
          <div class="flex items-center gap-2">
            <button @click="aprobarPedido" class="btn-success text-xs py-2 px-4 font-bold">
              ✓ Aprobar
            </button>
            <button @click="abrirRechazo" class="btn-danger text-xs py-2 px-4 font-bold">
              ✕ Rechazar
            </button>
          </div>
        </div>

        <!-- Información de Entrega si está aprobado -->
        <div v-if="entrega" class="border-t border-stone-100 pt-6">
          <div class="flex items-center justify-between border-b border-stone-50 pb-2 mb-4">
            <h2 class="text-base font-bold text-stone-900">Detalles de Entrega y Envío</h2>
            <RouterLink :to="`/entregas/${entrega.id}`" class="text-xs text-amber-600 hover:text-amber-700 font-bold">
              Ver Hoja de Ruta ➔
            </RouterLink>
          </div>
          <div class="grid grid-cols-2 gap-4 text-xs text-stone-600">
            <div>
              <p class="font-semibold text-stone-400 uppercase">Transportista</p>
              <p class="font-bold text-stone-800 mt-0.5">{{ entrega.transportista || 'Por asignar' }}</p>
            </div>
            <div>
              <p class="font-semibold text-stone-400 uppercase">Vehículo / Placa</p>
              <p class="font-medium text-stone-800 mt-0.5">{{ entrega.vehiculo || 'Por asignar' }}</p>
            </div>
            <div>
              <p class="font-semibold text-stone-400 uppercase">Estado Entrega</p>
              <div class="mt-1">
                <BadgeEstado :estado="entrega.estado" />
              </div>
            </div>
            <div>
              <p class="font-semibold text-stone-400 uppercase">Fecha Estimada Llegada</p>
              <p class="text-stone-700 mt-0.5">{{ formatFecha(pedido.fechaEntregaEstimada) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Timeline & Side Panel -->
      <div class="space-y-6">
        <!-- Visual timeline generated dynamically -->
        <div class="card bg-white p-6">
          <PedidoTimeline :historial="timelineHistorial" />
        </div>
      </div>
    </div>

    <!-- Confirm Modal para rechazo -->
    <ConfirmModal
      v-slot:default
      v-if="rechazoModal.mostrar"
      titulo="Rechazar Solicitud"
      confirmLabel="Rechazar"
      cancelLabel="Cancelar"
      :loading="rechazoModal.loading"
      @confirm="confirmarRechazo"
      @cancel="rechazoModal.mostrar = false"
    >
      <div class="space-y-3 text-left">
        <label class="block text-xs font-semibold text-stone-700">Por favor ingrese el motivo del rechazo:</label>
        <textarea
          v-model="rechazoModal.motivo"
          required
          rows="3"
          class="input-field text-xs resize-none"
          placeholder="Ej. Las dimensiones estructurales indicadas no son aptas..."
        ></textarea>
      </div>
    </ConfirmModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePedidosStore } from '@/stores/pedidos'
import BadgeEstado from '@/components/common/BadgeEstado.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'
import PedidoTimeline from '@/components/pedidos/PedidoTimeline.vue'
import api from '@/services/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const pedidosStore = usePedidosStore()

const pedidoId = route.params.id
const pedido = ref(null)
const entrega = ref(null)
const loading = ref(true)
const error = ref(null)

const rechazoModal = ref({
  mostrar: false,
  loading: false,
  motivo: ''
})

// Synthesize timeline from the pedido data structure
const timelineHistorial = computed(() => {
  if (!pedido.value) return []
  const list = []
  
  // 1. Creado siempre está
  list.push({
    id: 1,
    estadoNuevo: 'PENDIENTE',
    fechaCambio: pedido.value.fechaPedido,
    responsable: pedido.value.solicitadoPor,
    motivo: 'Solicitud inicial registrada.'
  })

  // 2. Si está rechazado
  if (pedido.value.estado === 'RECHAZADO') {
    list.push({
      id: 2,
      estadoAnterior: 'PENDIENTE',
      estadoNuevo: 'RECHAZADO',
      fechaCambio: new Date().toISOString(), // Fallback aproximado
      responsable: 'Jefe de Almacén',
      motivo: 'Rechazo: ' + (pedido.value.motivoRechazo || 'No especificado.')
    })
  }

  // 3. Si está aprobado o más
  const estadosAprobados = ['APROBADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO']
  if (estadosAprobados.includes(pedido.value.estado)) {
    list.push({
      id: 3,
      estadoAnterior: 'PENDIENTE',
      estadoNuevo: 'APROBADO',
      fechaCambio: pedido.value.fechaAprobacion || pedido.value.fechaPedido,
      responsable: pedido.value.aprobadoPor || 'Jefe de Almacén',
      motivo: 'Aprobación y autorización de despacho.'
    })
  }

  // 4. Si la entrega está en ruta
  if (entrega.value) {
    if (entrega.value.estado === 'PREPARANDO') {
      list.push({
        id: 4,
        estadoAnterior: 'APROBADO',
        estadoNuevo: 'EN_PREPARACION',
        fechaCambio: new Date().toISOString(),
        responsable: entrega.value.transportista || 'Personal de Carga',
        motivo: 'Carga de madera estructural en vehículo.'
      })
    }
    if (entrega.value.estado === 'EN_RUTA') {
      list.push({
        id: 5,
        estadoAnterior: 'EN_PREPARACION',
        estadoNuevo: 'DESPACHADO',
        fechaCambio: new Date().toISOString(),
        responsable: entrega.value.transportista || 'Transportista',
        motivo: 'Envío despachado hacia mina de destino.'
      })
    }
    if (entrega.value.estado === 'ENTREGADO') {
      list.push({
        id: 6,
        estadoAnterior: 'DESPACHADO',
        estadoNuevo: 'ENTREGADO',
        fechaCambio: new Date().toISOString(),
        responsable: 'Operaciones Mina',
        motivo: 'Recepción y conformidad del suministro.'
      })
    }
  }

  return list
})

async function cargarDetalle() {
  loading.value = true
  error.value = null
  try {
    const data = await pedidosStore.fetchPedido(pedidoId)
    pedido.value = data

    // Si está aprobado, buscar si tiene una entrega asociada
    const estadosAprobados = ['APROBADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO']
    if (estadosAprobados.includes(data.estado)) {
      try {
        const { data: listEntregas } = await api.get('/api/entregas')
        // Buscar la entrega por pedidoId (de tipo string o int)
        const match = listEntregas.find(e => String(e.pedidoId) === String(pedidoId))
        if (match) {
          entrega.value = match
        }
      } catch (errEntrega) {
        console.warn('No se pudo verificar la entrega asociada:', errEntrega)
      }
    }
  } catch (err) {
    console.error('Error fetching order details:', err)
    error.value = err.response?.data?.mensaje || 'Error al recuperar la información del pedido.'
  } finally {
    loading.value = false
  }
}

onMounted(cargarDetalle)

async function aprobarPedido() {
  if (!confirm('¿Desea aprobar este pedido?')) return
  loading.value = true
  try {
    await pedidosStore.aprobarPedido(pedidoId, authStore.usuario.email)
    await cargarDetalle()
  } catch (err) {
    alert(err.response?.data?.mensaje || 'Error aprobando pedido.')
    loading.value = false
  }
}

function abrirRechazo() {
  rechazoModal.value.motivo = ''
  rechazoModal.value.mostrar = true
}

async function confirmarRechazo() {
  if (!rechazoModal.value.motivo.trim()) {
    alert('Motivo requerido.')
    return
  }
  rechazoModal.value.loading = true
  try {
    await pedidosStore.rechazarPedido(pedidoId, rechazoModal.value.motivo)
    rechazoModal.value.mostrar = false
    await cargarDetalle()
  } catch (err) {
    alert(err.response?.data?.mensaje || 'Error rechazando pedido.')
  } finally {
    rechazoModal.value.loading = false
  }
}

function formatFecha(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  })
}

function formatFechaCompleta(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>
