<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Pedidos de Madera</h1>
        <p class="text-stone-500 text-sm mt-1">Solicitudes de suministro para reforzamiento y entibado de galerías.</p>
      </div>
      <RouterLink
        v-if="authStore.esCompras || authStore.esAdmin"
        to="/pedidos/nuevo"
        class="btn-primary flex items-center gap-2"
      >
        <span>+</span> Solicitar Madera
      </RouterLink>
    </div>

    <!-- Filtros -->
    <div class="card p-4">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5 font-medium">Sede / Mina</label>
          <input
            v-model="filtros.mina"
            placeholder="Ej. Yanacocha..."
            class="input-field"
          />
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5 font-medium">Estado del Pedido</label>
          <select v-model="filtros.estado" class="input-field select-custom">
            <option value="">Todos los estados</option>
            <option value="PENDIENTE">Pendiente</option>
            <option value="APROBADO">Aprobado</option>
            <option value="EN_PREPARACION">En preparación</option>
            <option value="DESPACHADO">Despachado</option>
            <option value="ENTREGADO">Entregado</option>
            <option value="RECHAZADO">Rechazado</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5 font-medium">Ordenar por fecha</label>
          <select v-model="filtros.orden" class="input-field select-custom">
            <option value="desc">Más recientes primero</option>
            <option value="asc">Más antiguos primero</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Errores del Store -->
    <AlertMessage v-if="pedidosStore.error" tipo="error">
      {{ pedidosStore.error }}
    </AlertMessage>

    <!-- Table or Loader -->
    <div v-if="pedidosStore.loading" class="min-h-[250px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <div v-else class="card p-0 overflow-hidden shadow-sm border border-stone-100 bg-white">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm border-collapse">
          <thead>
            <tr class="bg-stone-50/80 border-b border-stone-200">
              <th class="table-header">ID</th>
              <th class="table-header">Madera Solicitada</th>
              <th class="table-header">Cantidad</th>
              <th class="table-header">Mina / Sede</th>
              <th class="table-header">Solicitante</th>
              <th class="table-header">Fecha Registro</th>
              <th class="table-header">Estado</th>
              <th class="table-header text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-100">
            <tr v-for="pedido in pedidosOrdenados" :key="pedido.id" class="hover:bg-stone-50/50 transition-colors">
              <td class="table-cell font-semibold text-stone-900">#{{ pedido.id }}</td>
              <td class="table-cell">
                <span class="font-semibold text-stone-850 capitalize">{{ pedido.tipoMadera }}</span>
                <span class="text-xs text-stone-400 block mt-0.5 capitalize">Área: {{ pedido.areaSolicitante }}</span>
              </td>
              <td class="table-cell font-bold text-stone-850">
                {{ pedido.cantidadSolicitada }} <span class="text-xs font-normal text-stone-400 capitalize">{{ pedido.unidad }}</span>
              </td>
              <td class="table-cell text-stone-500 font-medium">{{ pedido.mina }}</td>
              <td class="table-cell text-stone-600 truncate max-w-[120px]" :title="pedido.solicitadoPor">
                {{ pedido.solicitadoPor }}
              </td>
              <td class="table-cell text-stone-400 text-xs">
                {{ formatFecha(pedido.fechaPedido) }}
              </td>
              <td class="table-cell">
                <BadgeEstado :estado="pedido.estado" />
              </td>
              <td class="table-cell text-right">
                <div class="flex items-center justify-end gap-2">
                  <RouterLink
                    :to="`/pedidos/${pedido.id}`"
                    class="text-xs text-stone-500 hover:text-stone-800 font-semibold border border-stone-200 hover:border-stone-300 px-2 py-1.5 rounded-lg transition-all"
                  >
                    🔍 Ver Detalle
                  </RouterLink>

                  <!-- Acciones de Almacén sobre pedidos Pendientes -->
                  <button
                    v-if="authStore.esAlmacen && pedido.estado === 'PENDIENTE'"
                    @click="confirmarAprobacion(pedido)"
                    class="text-xs text-emerald-600 hover:text-emerald-800 font-semibold border border-emerald-200 hover:border-emerald-300 bg-emerald-50/20 px-2 py-1.5 rounded-lg transition-all"
                  >
                    ✓ Aprobar
                  </button>
                  <button
                    v-if="authStore.esAlmacen && pedido.estado === 'PENDIENTE'"
                    @click="abrirRechazoModal(pedido)"
                    class="text-xs text-rose-600 hover:text-rose-800 font-semibold border border-rose-200 hover:border-rose-300 bg-rose-50/20 px-2 py-1.5 rounded-lg transition-all"
                  >
                    ✕ Rechazar
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="pedidosOrdenados.length === 0">
              <td colspan="8" class="text-center py-12 text-stone-400 italic">
                No se encontraron pedidos registrados.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal de Confirmación para Aprobación -->
    <ConfirmModal
      v-slot:default
      v-if="aprobacionModal.mostrar"
      titulo="Confirmar Aprobación de Pedido"
      confirmLabel="Sí, Aprobar Pedido"
      cancelLabel="Cancelar"
      :loading="aprobacionModal.loading"
      @confirm="ejecutarAprobacion"
      @cancel="aprobacionModal.mostrar = false"
    >
      <div class="text-left text-xs text-stone-600 space-y-2">
        <p>Estás a punto de aprobar el pedido <b>#{{ aprobacionModal.pedido.id }}</b>.</p>
        <p><b>Madera:</b> {{ aprobacionModal.pedido.tipoMadera }} ({{ aprobacionModal.pedido.cantidadSolicitada }} {{ aprobacionModal.pedido.unidad }})</p>
        <p><b>Destino:</b> {{ aprobacionModal.pedido.mina }} — {{ aprobacionModal.pedido.areaSolicitante }}</p>
        <div class="bg-amber-50 border border-amber-100 rounded-xl p-3 text-amber-850 mt-4">
          ⚠️ <b>Nota:</b> Al aprobar, se descontará automáticamente el stock del inventario correspondiente. Si el inventario no cuenta con stock suficiente, la operación lanzará un error.
        </div>
      </div>
    </ConfirmModal>

    <!-- Modal para Rechazar Pedido -->
    <ConfirmModal
      v-if="rechazoModal.mostrar"
      titulo="Rechazar Solicitud de Pedido"
      confirmLabel="Confirmar Rechazo"
      cancelLabel="Cancelar"
      :loading="rechazoModal.loading"
      @confirm="ejecutarRechazo"
      @cancel="rechazoModal.mostrar = false"
    >
      <template #body>
        <div class="space-y-4 text-left text-xs">
          <p class="text-stone-500">
            Ingresa el motivo detallado de rechazo para el pedido <b>#{{ rechazoModal.pedido.id }}</b>. Esto se registrará en el historial y se enviará al departamento de compras.
          </p>
          <div>
            <label class="block text-xs font-semibold text-stone-700 mb-1.5">Motivo del Rechazo</label>
            <textarea
              v-model="rechazoModal.motivo"
              required
              rows="3"
              class="input-field resize-none text-xs"
              placeholder="Ej. Stock insuficiente en almacén central o medidas incorrectas solicitadas..."
            ></textarea>
          </div>
        </div>
      </template>
    </ConfirmModal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePedidosStore } from '@/stores/pedidos'
import BadgeEstado from '@/components/common/BadgeEstado.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

const authStore = useAuthStore()
const pedidosStore = usePedidosStore()

const filtros = reactive({
  mina: '',
  estado: '',
  orden: 'desc'
})

onMounted(async () => {
  await pedidosStore.fetchPedidos()
})

const pedidosFiltrados = computed(() => {
  return pedidosStore.pedidos.filter(p => {
    const coincideMina = !filtros.mina || p.mina.toLowerCase().includes(filtros.mina.toLowerCase())
    const coincideEstado = !filtros.estado || p.estado === filtros.estado
    return coincideMina && coincideEstado
  })
})

const pedidosOrdenados = computed(() => {
  const result = [...pedidosFiltrados.value]
  result.sort((a, b) => {
    const da = new Date(a.fechaPedido || 0).getTime()
    const db = new Date(b.fechaPedido || 0).getTime()
    return filtros.orden === 'desc' ? db - da : da - db
  })
  return result
})

function formatFecha(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Lógica de Aprobación
const aprobacionModal = reactive({
  mostrar: false,
  loading: false,
  pedido: null
})

function confirmarAprobacion(pedido) {
  aprobacionModal.pedido = pedido
  aprobacionModal.mostrar = true
}

async function ejecutarAprobacion() {
  aprobacionModal.loading = true
  try {
    await pedidosStore.aprobarPedido(aprobacionModal.pedido.id, authStore.usuario.email)
    aprobacionModal.mostrar = false
  } catch (err) {
    alert(err.response?.data?.mensaje || 'Error al aprobar el pedido.')
  } finally {
    aprobacionModal.loading = false
  }
}

// Lógica de Rechazo
const rechazoModal = reactive({
  mostrar: false,
  loading: false,
  pedido: null,
  motivo: ''
})

function abrirRechazoModal(pedido) {
  rechazoModal.pedido = pedido
  rechazoModal.motivo = ''
  rechazoModal.mostrar = true
}

async function ejecutarRechazo() {
  if (!rechazoModal.motivo.trim()) {
    alert('Por favor especifique un motivo.')
    return
  }

  rechazoModal.loading = true
  try {
    await pedidosStore.rechazarPedido(rechazoModal.pedido.id, rechazoModal.motivo)
    rechazoModal.mostrar = false
  } catch (err) {
    alert(err.response?.data?.mensaje || 'Error al rechazar el pedido.')
  } finally {
    rechazoModal.loading = false
  }
}
</script>

<style scoped>
.select-custom {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3E%3Cpath stroke='%236B7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='m6 8 4 4 4-4'/%3E%3C/svg%3E");
  background-position: right 0.5rem center;
  background-repeat: no-repeat;
  background-size: 1.25em 1.25em;
  padding-right: 2rem;
}
</style>
